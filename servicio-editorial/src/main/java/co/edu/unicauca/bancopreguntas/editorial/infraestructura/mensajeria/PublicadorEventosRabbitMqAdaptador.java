package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.correlacion.ContextoCorrelacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Publica en RabbitMQ los eventos de integración de Editorial (CONTRATOS.md 7, comunicación C3). Implementa
 * {@link PublicadorEventosPuerto}.
 *
 * <ul>
 *   <li>Solo {@code PreguntaPublicada} y {@code PreguntaArchivada} salen al exchange {@code editorial.eventos}, con
 *       las routing keys {@code pregunta.publicada} y {@code pregunta.archivada}. Los demás eventos de dominio solo
 *       se registran en el log.</li>
 *   <li>Se publica <strong>después del commit</strong> (7.7.1): si hay una transacción activa, el envío se registra
 *       como {@link TransactionSynchronization#afterCommit()}. Si la transacción se revierte, no sale nada.</li>
 *   <li>Propiedades AMQP de 7.2: {@code content_type}, {@code content_encoding}, {@code delivery_mode=2},
 *       {@code message_id = idEvento}, {@code type = tipoEvento} y {@code timestamp}.</li>
 *   <li><em>Publisher confirms</em> (7.7.2): el {@code idEvento} viaja como {@link CorrelationData}. Si el broker no
 *       confirma, el callback de la configuración lo registra en el log.</li>
 * </ul>
 */
public class PublicadorEventosRabbitMqAdaptador implements PublicadorEventosPuerto {

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorEventosRabbitMqAdaptador.class);

    private final RabbitTemplate rabbit;
    private final TraductorEventosIntegracion traductor;
    private final Clock reloj;

    /**
     * Crea el adaptador.
     *
     * @param rabbit    plantilla de RabbitMQ con publisher confirms
     * @param traductor traductor a los mensajes de integración
     * @param reloj     reloj UTC para la propiedad {@code timestamp}
     */
    public PublicadorEventosRabbitMqAdaptador(RabbitTemplate rabbit, TraductorEventosIntegracion traductor, Clock reloj) {
        this.rabbit = rabbit;
        this.traductor = traductor;
        this.reloj = reloj;
    }

    @Override
    public void publicar(List<EventoDeDominio> eventos) {
        String idCorrelacion = ContextoCorrelacion.actual().orElse(null);
        List<MensajeIntegracion> mensajes = new ArrayList<>();
        for (EventoDeDominio evento : eventos) {
            traductor.traducir(evento, idCorrelacion).ifPresentOrElse(mensajes::add,
                    () -> LOG.info("Evento de dominio {} registrado; no sale al broker: {}",
                            evento.getClass().getSimpleName(), evento));
        }
        if (mensajes.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // CONTRATOS.md 7.7.1: publicar solo si la transacción se confirma.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mensajes.forEach(PublicadorEventosRabbitMqAdaptador.this::enviar);
                }
            });
        } else {
            mensajes.forEach(this::enviar);
        }
    }

    private void enviar(MensajeIntegracion mensaje) {
        Message amqp = MessageBuilder.withBody(mensaje.cuerpo())
                .setContentType("application/json")
                .setContentEncoding("utf-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(mensaje.idEvento())
                .setType(mensaje.tipoEvento())
                .setTimestamp(Date.from(reloj.instant()))
                .build();
        try {
            rabbit.send(TraductorEventosIntegracion.EXCHANGE, mensaje.routingKey(), amqp, new CorrelationData(mensaje.idEvento()));
            LOG.info("Evento {} publicado en {} con routing key {} (idEvento={})", mensaje.tipoEvento(),
                    TraductorEventosIntegracion.EXCHANGE, mensaje.routingKey(), mensaje.idEvento());
        } catch (AmqpException error) {
            // La transacción ya se confirmó: no se puede revertir. Se deja el rastro con el idEvento (7.7.2).
            LOG.error("No se pudo publicar el evento {} (idEvento={}): {}", mensaje.tipoEvento(), mensaje.idEvento(),
                    error.getMessage());
        }
    }
}
