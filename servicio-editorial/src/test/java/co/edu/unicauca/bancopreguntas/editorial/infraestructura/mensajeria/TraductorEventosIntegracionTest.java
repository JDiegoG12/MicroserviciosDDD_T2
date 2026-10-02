package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaAprobada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.MotivoDeArchivado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.ValidadorDeContratos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Optional;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.ADMINISTRADOR;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.preguntaEnEstado;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba del productor contra el contrato (CONTRATOS.md 7.8): el JSON que genera el código para cada evento se valida
 * contra {@code /contratos/eventos/editorial/*.schema.json}, y las fechas salen en texto ISO-8601 UTC con {@code Z}
 * (Jackson 3, CONTRATOS.md 4).
 */
@DisplayName("TraductorEventosIntegracion (CONTRATOS.md 7.3 a 7.5)")
class TraductorEventosIntegracionTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant PUBLICACION = Instant.parse("2026-10-01T15:30:00.123Z");
    private static final String CORRELACION = "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b";

    private final TraductorEventosIntegracion traductor = new TraductorEventosIntegracion(JSON);

    private static EventoDeDominio eventoDePublicacion() {
        Pregunta pregunta = preguntaEnEstado(EstadoPregunta.APROBADA);
        pregunta.publicar(ADMINISTRADOR, PUBLICACION);
        return pregunta.extraerEventos().get(0);
    }

    private static EventoDeDominio eventoDeArchivado() {
        Pregunta pregunta = preguntaEnEstado(EstadoPregunta.PUBLICADA);
        pregunta.archivar(new MotivoDeArchivado("Contenido desactualizado"), ADMINISTRADOR, PUBLICACION);
        return pregunta.extraerEventos().get(0);
    }

    @Test
    @DisplayName("PreguntaPublicada cumple pregunta-publicada.v1.schema.json, con y sin idCorrelacion")
    void preguntaPublicadaCumpleElEsquema() {
        MensajeIntegracion conCorrelacion = traductor.traducir(eventoDePublicacion(), CORRELACION).orElseThrow();
        MensajeIntegracion sinCorrelacion = traductor.traducir(eventoDePublicacion(), null).orElseThrow();

        assertThat(ValidadorDeContratos.validar("editorial/pregunta-publicada.v1.schema.json", conCorrelacion.cuerpo())).isEmpty();
        assertThat(ValidadorDeContratos.validar("editorial/pregunta-publicada.v1.schema.json", sinCorrelacion.cuerpo())).isEmpty();
        assertThat(conCorrelacion.routingKey()).isEqualTo("pregunta.publicada");
        assertThat(conCorrelacion.tipoEvento()).isEqualTo("PreguntaPublicada");
    }

    @Test
    @DisplayName("PreguntaArchivada cumple pregunta-archivada.v1.schema.json y lleva el motivo")
    void preguntaArchivadaCumpleElEsquema() {
        MensajeIntegracion mensaje = traductor.traducir(eventoDeArchivado(), CORRELACION).orElseThrow();

        assertThat(ValidadorDeContratos.validar("editorial/pregunta-archivada.v1.schema.json", mensaje.cuerpo())).isEmpty();
        assertThat(mensaje.routingKey()).isEqualTo("pregunta.archivada");
        assertThat(JSON.readTree(mensaje.cuerpo()).at("/datos/motivo").asString()).isEqualTo("Contenido desactualizado");
    }

    @Test
    @DisplayName("El sobre lleva idEvento, versión 1, origen, correlación y fechas ISO-8601 UTC con Z (Jackson 3)")
    void sobreYFechas() {
        MensajeIntegracion mensaje = traductor.traducir(eventoDePublicacion(), CORRELACION).orElseThrow();
        JsonNode sobre = JSON.readTree(mensaje.cuerpo());

        assertThat(sobre.get("idEvento").asString()).isEqualTo(mensaje.idEvento());
        assertThat(sobre.get("versionEvento").asInt()).isEqualTo(1);
        assertThat(sobre.get("origen").asString()).isEqualTo("servicio-editorial");
        assertThat(sobre.get("idCorrelacion").asString()).isEqualTo(CORRELACION);
        assertThat(sobre.get("fechaOcurrencia").isString()).isTrue();
        assertThat(sobre.get("fechaOcurrencia").asString()).isEqualTo("2026-10-01T15:30:00.123Z");
        assertThat(sobre.at("/datos/fechaPublicacion").asString()).isEqualTo("2026-10-01T15:30:00.123Z");
        assertThat(sobre.at("/datos/letraCorrecta").asString()).isEqualTo("C");
    }

    @Test
    @DisplayName("Sin correlación el campo idCorrelacion se envía como null (no se omite, CONTRATOS.md 4)")
    void correlacionNulaNoSeOmite() {
        JsonNode sobre = JSON.readTree(traductor.traducir(eventoDePublicacion(), null).orElseThrow().cuerpo());
        assertThat(sobre.has("idCorrelacion")).isTrue();
        assertThat(sobre.get("idCorrelacion").isNull()).isTrue();
    }

    @Test
    @DisplayName("El validador del contrato sí rechaza un mensaje con un campo extra o sin datos (esquema estricto)")
    void elValidadorRechazaMensajesInvalidos() {
        String conCampoExtra = new String(traductor.traducir(eventoDePublicacion(), null).orElseThrow().cuerpo())
                .replaceFirst("\\{", "{\"campoExtra\":1,");
        assertThat(ValidadorDeContratos.validar("editorial/pregunta-publicada.v1.schema.json", conCampoExtra.getBytes())).isNotEmpty();
        assertThat(ValidadorDeContratos.validar("editorial/pregunta-publicada.v1.schema.json", "{}".getBytes())).isNotEmpty();
    }

    @Test
    @DisplayName("Los demás eventos de dominio no tienen mensaje de integración")
    void otrosEventosNoSalen() {
        Optional<MensajeIntegracion> mensaje = traductor.traducir(new PreguntaAprobada(PreguntaId.generar(), PUBLICACION), null);
        assertThat(mensaje).isEmpty();
    }
}
