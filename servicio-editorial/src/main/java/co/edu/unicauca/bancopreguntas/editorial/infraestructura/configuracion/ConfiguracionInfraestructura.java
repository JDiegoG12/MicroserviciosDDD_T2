package co.edu.unicauca.bancopreguntas.editorial.infraestructura.configuracion;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.catalogo.CatalogoAcademicoGrpcAdaptador;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria.DeclaradorTopologiaRabbitMq;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria.PublicadorEventosRabbitMqAdaptador;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria.TraductorEventosIntegracion;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.MigradorEsquemaEnSegundoPlano;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.PreguntaJpaSpring;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.PreguntaRepositorioJpa;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.ProcesoDeRevisionRepositorioJpa;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.ProcesoRevisionJpaSpring;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.reloj.RelojUtc;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.transaccion.EjecutorTransaccional;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;

/**
 * Cableado de los adaptadores de salida: repositorios JPA, reloj, cliente gRPC de Catálogo y publicador RabbitMQ.
 * Los valores vienen de las variables de entorno de CONTRATOS.md 9.2 (ver {@code application.yml}).
 */
@Configuration
public class ConfiguracionInfraestructura {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionInfraestructura.class);

    /**
     * Reloj del sistema en UTC.
     *
     * @return reloj UTC
     */
    @Bean
    public Clock relojDelSistema() {
        return Clock.systemUTC();
    }

    /**
     * Puerto de reloj de los casos de uso.
     *
     * @param reloj reloj UTC
     * @return reloj de aplicación
     */
    @Bean
    public RelojPuerto relojPuerto(Clock reloj) {
        return new RelojUtc(reloj);
    }

    /**
     * Decorador transaccional de los casos de uso.
     *
     * @param gestorDeTransacciones gestor de transacciones de JPA
     * @return el ejecutor
     */
    @Bean
    public EjecutorTransaccional ejecutorTransaccional(PlatformTransactionManager gestorDeTransacciones) {
        return new EjecutorTransaccional(gestorDeTransacciones);
    }

    /**
     * Migraciones de Flyway en segundo plano y estado del esquema (CONTRATOS.md 9.3.6).
     *
     * @param fuenteDeDatos conexión a PostgreSQL
     * @param esperaInicial espera antes del segundo intento
     * @param esperaMaxima  tope de espera entre intentos
     * @return el migrador, que también informa si el esquema está listo
     */
    @Bean
    public MigradorEsquemaEnSegundoPlano migradorEsquemaEnSegundoPlano(
            DataSource fuenteDeDatos,
            @Value("${editorial.migraciones.espera-inicial:1s}") Duration esperaInicial,
            @Value("${editorial.migraciones.espera-maxima:30s}") Duration esperaMaxima) {
        return new MigradorEsquemaEnSegundoPlano(fuenteDeDatos, esperaInicial, esperaMaxima);
    }

    /**
     * Repositorio de Preguntas sobre PostgreSQL.
     *
     * @param repositorio repositorio técnico de Spring Data
     * @return implementación JPA
     */
    @Bean
    public PreguntaRepositorio preguntaRepositorio(PreguntaJpaSpring repositorio) {
        return new PreguntaRepositorioJpa(repositorio);
    }

    /**
     * Repositorio de Procesos de revisión sobre PostgreSQL.
     *
     * @param repositorio repositorio técnico de Spring Data
     * @return implementación JPA
     */
    @Bean
    public ProcesoDeRevisionRepositorio procesoDeRevisionRepositorio(ProcesoRevisionJpaSpring repositorio) {
        return new ProcesoDeRevisionRepositorioJpa(repositorio);
    }

    /**
     * Canal gRPC hacia Catálogo: texto plano y perezoso (se conecta en la primera llamada; CONTRATOS.md 6 y 9.3.6).
     *
     * @param host   {@code CATALOGO_GRPC_HOST}
     * @param puerto {@code CATALOGO_GRPC_PUERTO}
     * @return canal gRPC; se cierra al detener el servicio
     */
    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel canalCatalogo(@Value("${editorial.catalogo.grpc.host}") String host,
                                        @Value("${editorial.catalogo.grpc.puerto}") int puerto) {
        return ManagedChannelBuilder.forAddress(host, puerto).usePlaintext().build();
    }

    /**
     * Puerto de Catálogo implementado con gRPC.
     *
     * @param canal       canal gRPC
     * @param plazoMaximo deadline por llamada (2 s por defecto)
     * @return adaptador gRPC
     */
    @Bean
    public CatalogoAcademicoPuerto catalogoAcademicoPuerto(ManagedChannel canal,
                                                           @Value("${editorial.catalogo.grpc.plazo-maximo:2s}") Duration plazoMaximo) {
        return new CatalogoAcademicoGrpcAdaptador(canal, plazoMaximo);
    }

    /**
     * Exchange {@code editorial.eventos}: {@code topic}, {@code durable=true}, {@code autoDelete=false}, exactamente
     * como en CONTRATOS.md 7.1. Lo declara RabbitAdmin al abrir la primera conexión, de forma idempotente.
     *
     * @return el exchange
     */
    @Bean
    public TopicExchange exchangeEditorial() {
        return new TopicExchange(TraductorEventosIntegracion.EXCHANGE, true, false);
    }

    /**
     * Declara el exchange al arrancar, en segundo plano y con reintentos (CONTRATOS.md 7.1 y 7.7.6).
     *
     * @param administrador administrador AMQP
     * @param exchange      exchange {@code editorial.eventos}
     * @return el declarador
     */
    @Bean
    public DeclaradorTopologiaRabbitMq declaradorTopologiaRabbitMq(AmqpAdmin administrador, TopicExchange exchange) {
        return new DeclaradorTopologiaRabbitMq(administrador, exchange);
    }

    /**
     * Activa el callback de <em>publisher confirms</em> (CONTRATOS.md 7.7.2): si RabbitMQ no confirma un mensaje,
     * se registra el {@code idEvento} en el log.
     *
     * @return personalizador de la plantilla de RabbitMQ
     */
    @Bean
    public RabbitTemplateCustomizer callbackDeConfirmaciones() {
        return plantilla -> plantilla.setConfirmCallback((correlacion, confirmado, causa) -> {
            if (!confirmado) {
                LOG.error("RabbitMQ no confirmó el evento idEvento={}: {}",
                        correlacion == null ? "desconocido" : correlacion.getId(), causa);
            }
        });
    }

    /**
     * Traductor a los mensajes de integración, con su propio serializador Jackson 3 para que el JSON no dependa de
     * la configuración de la API REST (fechas en texto ISO-8601 UTC con {@code Z}).
     *
     * @return el traductor
     */
    @Bean
    public TraductorEventosIntegracion traductorEventosIntegracion() {
        return new TraductorEventosIntegracion(JsonMapper.builder().build());
    }

    /**
     * Puerto de publicación de eventos implementado con RabbitMQ.
     *
     * @param rabbit    plantilla de RabbitMQ
     * @param traductor traductor de eventos
     * @param reloj     reloj UTC
     * @return adaptador RabbitMQ
     */
    @Bean
    public PublicadorEventosPuerto publicadorEventosPuerto(RabbitTemplate rabbit, TraductorEventosIntegracion traductor,
                                                           Clock reloj) {
        return new PublicadorEventosRabbitMqAdaptador(rabbit, traductor, reloj);
    }
}
