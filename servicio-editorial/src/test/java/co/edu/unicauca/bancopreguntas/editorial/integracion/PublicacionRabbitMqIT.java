package co.edu.unicauca.bancopreguntas.editorial.integracion;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.PublicarPreguntaCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ArchivarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ArchivarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.PublicadorPreguntaServicio;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.ValidadorDeContratos;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.correlacion.ContextoCorrelacion;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.transaccion.EjecutorTransaccional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Publicador RabbitMQ contra un broker real (Testcontainers): el exchange {@code editorial.eventos} declarado como en
 * 7.1, un mensaje válido contra el esquema estricto en una cola de prueba enlazada a {@code pregunta.publicada}, las
 * propiedades AMQP de 7.2, y ningún mensaje si el caso de uso falla después de guardar (7.7.1).
 */
@DisplayName("Publicador RabbitMQ contra RabbitMQ 3.13 (Testcontainers)")
class PublicacionRabbitMqIT extends PruebaDeIntegracion {

    private static final long ESPERA_MS = 5_000;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    RabbitTemplate rabbit;
    @Autowired
    AmqpAdmin administrador;
    @Autowired
    TopicExchange exchangeEditorial;
    @Autowired
    ArchivarPregunta archivarPregunta;
    @Autowired
    PreguntaRepositorio preguntaRepositorio;
    @Autowired
    ProcesoDeRevisionRepositorio procesoRepositorio;
    @Autowired
    PublicadorEventosPuerto publicadorEventos;
    @Autowired
    RelojPuerto reloj;
    @Autowired
    EjecutorTransaccional transaccion;
    @Autowired
    JdbcTemplate jdbc;

    private String colaPublicadas;
    private String colaArchivadas;

    @BeforeEach
    void declararColasDePrueba() {
        administrador.declareExchange(exchangeEditorial);
        colaPublicadas = "prueba.publicadas." + UUID.randomUUID();
        colaArchivadas = "prueba.archivadas." + UUID.randomUUID();
        Queue publicadas = new Queue(colaPublicadas, false, true, true);
        Queue archivadas = new Queue(colaArchivadas, false, true, true);
        administrador.declareQueue(publicadas);
        administrador.declareQueue(archivadas);
        administrador.declareBinding(BindingBuilder.bind(publicadas).to(exchangeEditorial).with("pregunta.publicada"));
        administrador.declareBinding(BindingBuilder.bind(archivadas).to(exchangeEditorial).with("pregunta.archivada"));
    }

    @AfterEach
    void limpiarCorrelacion() {
        ContextoCorrelacion.limpiar();
    }

    @Test
    @DisplayName("El exchange editorial.eventos existe como topic, durable y sin autoDelete")
    void exchangeDeclarado() {
        assertThat(exchangeEditorial.getType()).isEqualTo("topic");
        assertThat(exchangeEditorial.isDurable()).isTrue();
        assertThat(exchangeEditorial.isAutoDelete()).isFalse();
        assertThat(administrador.getQueueInfo(colaPublicadas)).isNotNull();
    }

    @Test
    @DisplayName("Publicar deja en la cola de prueba un PreguntaPublicada válido contra el esquema y con las propiedades de 7.2")
    void publicarEmiteMensajeValido() {
        String preguntaId = crearAprobada(usuarioNuevo(Rol.AUTOR), UUID.randomUUID());
        ContextoCorrelacion.establecer("0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b");

        publicarPregunta.ejecutar(ADMINISTRADOR, preguntaId);

        Message mensaje = rabbit.receive(colaPublicadas, ESPERA_MS);
        assertThat(mensaje).isNotNull();
        assertThat(ValidadorDeContratos.validar("editorial/pregunta-publicada.v1.schema.json", mensaje.getBody())).isEmpty();
        JsonNode sobre = JSON.readTree(mensaje.getBody());
        assertThat(sobre.at("/datos/preguntaId").asString()).isEqualTo(preguntaId);
        assertThat(sobre.get("idCorrelacion").asString()).isEqualTo("0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b");
        assertThat(mensaje.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(mensaje.getMessageProperties().getContentEncoding()).isEqualTo("utf-8");
        assertThat(mensaje.getMessageProperties().getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(mensaje.getMessageProperties().getMessageId()).isEqualTo(sobre.get("idEvento").asString());
        assertThat(mensaje.getMessageProperties().getType()).isEqualTo("PreguntaPublicada");
        assertThat(mensaje.getMessageProperties().getTimestamp()).isNotNull();
        assertThat(rabbit.receive(colaArchivadas, 500)).isNull();
    }

    @Test
    @DisplayName("Archivar deja un PreguntaArchivada válido con su motivo en la cola enlazada a pregunta.archivada")
    void archivarEmiteMensajeValido() {
        String preguntaId = crearAprobada(usuarioNuevo(Rol.AUTOR), UUID.randomUUID());
        publicarPregunta.ejecutar(ADMINISTRADOR, preguntaId);
        rabbit.receive(colaPublicadas, ESPERA_MS);

        archivarPregunta.ejecutar(ADMINISTRADOR, new ArchivarPreguntaComando(preguntaId, "Contenido desactualizado"));

        Message mensaje = rabbit.receive(colaArchivadas, ESPERA_MS);
        assertThat(mensaje).isNotNull();
        assertThat(ValidadorDeContratos.validar("editorial/pregunta-archivada.v1.schema.json", mensaje.getBody())).isEmpty();
        assertThat(JSON.readTree(mensaje.getBody()).at("/datos/motivo").asString()).isEqualTo("Contenido desactualizado");
    }

    @Test
    @DisplayName("Si el caso de uso falla después de guardar y de pedir la publicación, no sale ningún mensaje (7.7.1)")
    void fallaDespuesDeGuardarNoPublica() {
        String preguntaId = crearAprobada(usuarioNuevo(Rol.AUTOR), UUID.randomUUID());
        PublicadorEventosPuerto publicadorQueFallaDespues = eventos -> {
            publicadorEventos.publicar(eventos);
            throw new IllegalStateException("Falla simulada después de guardar y publicar");
        };
        PublicarPreguntaCasoUso casoDeUso = new PublicarPreguntaCasoUso(preguntaRepositorio, procesoRepositorio,
                new PublicadorPreguntaServicio(), publicadorQueFallaDespues, reloj);

        assertThatThrownBy(() -> transaccion.escribir(() -> casoDeUso.ejecutar(ADMINISTRADOR, preguntaId)))
                .hasMessageContaining("Falla simulada");

        assertThat(rabbit.receive(colaPublicadas, 1_500)).isNull();
        String estado = jdbc.queryForObject("select estado from pregunta where id = ?", String.class, UUID.fromString(preguntaId));
        assertThat(estado).isEqualTo(EstadoPregunta.APROBADA.name());
    }

    @Test
    @DisplayName("Los demás eventos de dominio no salen al broker")
    void otrosEventosNoSalen() {
        crearAprobada(usuarioNuevo(Rol.AUTOR), UUID.randomUUID());
        assertThat(rabbit.receive(colaPublicadas, 1_000)).isNull();
        assertThat(rabbit.receive(colaArchivadas, 200)).isNull();
    }

}
