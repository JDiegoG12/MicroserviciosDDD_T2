package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.Duration;

/**
 * Declara el exchange {@code editorial.eventos} en cuanto el servicio arranca, sin esperar al primer mensaje
 * (CONTRATOS.md 7.1: lo declaran Editorial y Evaluación, de forma idempotente y con los mismos argumentos).
 *
 * <p>Corre en un hilo aparte y reintenta con espera progresiva (máximo 30 s entre intentos), así el servicio arranca
 * y queda sano aunque RabbitMQ aún no esté listo (CONTRATOS.md 7.7.6 y 9.3.6).</p>
 */
public class DeclaradorTopologiaRabbitMq {

    private static final Logger LOG = LoggerFactory.getLogger(DeclaradorTopologiaRabbitMq.class);
    private static final Duration ESPERA_INICIAL = Duration.ofSeconds(1);
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(30);

    private final AmqpAdmin administrador;
    private final TopicExchange exchange;

    /**
     * Crea el declarador.
     *
     * @param administrador administrador AMQP
     * @param exchange      exchange {@code editorial.eventos}
     */
    public DeclaradorTopologiaRabbitMq(AmqpAdmin administrador, TopicExchange exchange) {
        this.administrador = administrador;
        this.exchange = exchange;
    }

    /**
     * Lanza la declaración en segundo plano cuando la aplicación está lista.
     *
     * @param evento aplicación lista
     */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar(ApplicationReadyEvent evento) {
        Thread.ofVirtual().name("declarador-rabbitmq").start(this::declararConReintentos);
    }

    private void declararConReintentos() {
        Duration espera = ESPERA_INICIAL;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                administrador.declareExchange(exchange);
                LOG.info("Exchange {} declarado (topic, durable, sin autoDelete).", exchange.getName());
                return;
            } catch (AmqpException error) {
                LOG.warn("RabbitMQ no disponible para declarar {}; nuevo intento en {} s: {}", exchange.getName(),
                        espera.toSeconds(), error.getMessage());
                dormir(espera);
                espera = espera.multipliedBy(2).compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera.multipliedBy(2);
            }
        }
    }

    private static void dormir(Duration espera) {
        try {
            Thread.sleep(espera);
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
        }
    }
}
