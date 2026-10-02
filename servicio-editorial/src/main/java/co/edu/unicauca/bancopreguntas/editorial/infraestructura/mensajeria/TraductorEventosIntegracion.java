package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaArchivada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaPublicada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Traduce los eventos de dominio que salen del servicio al mensaje de integración de CONTRATOS.md 7.3 a 7.5
 * (CONTRATOS.md 3.3.6: la traducción ocurre en infraestructura, nunca en el dominio).
 *
 * <p>Solo {@code PreguntaPublicada} y {@code PreguntaArchivada} tienen mensaje de integración; los demás eventos de
 * dominio no salen al broker.</p>
 */
public class TraductorEventosIntegracion {

    /** Exchange de Editorial (CONTRATOS.md 7.1). */
    public static final String EXCHANGE = "editorial.eventos";
    /** Routing key de {@code PreguntaPublicada} (CONTRATOS.md 7.4). */
    public static final String RUTA_PREGUNTA_PUBLICADA = "pregunta.publicada";
    /** Routing key de {@code PreguntaArchivada} (CONTRATOS.md 7.5). */
    public static final String RUTA_PREGUNTA_ARCHIVADA = "pregunta.archivada";
    /** Valor fijo del campo {@code origen} (CONTRATOS.md 7.3). */
    public static final String ORIGEN = "servicio-editorial";
    /** Versión del contrato de los eventos (CONTRATOS.md 7.3). */
    public static final int VERSION_EVENTO = 1;

    private final JsonMapper json;
    private final Supplier<String> generadorIdEvento;

    /**
     * Crea el traductor con un generador de {@code idEvento} UUID v4.
     *
     * @param json serializador Jackson 3 (fechas en texto ISO-8601)
     */
    public TraductorEventosIntegracion(JsonMapper json) {
        this(json, () -> UUID.randomUUID().toString());
    }

    /**
     * Crea el traductor con un generador de {@code idEvento} propio (para pruebas deterministas).
     *
     * @param json              serializador Jackson 3
     * @param generadorIdEvento generador de identificadores de evento
     */
    public TraductorEventosIntegracion(JsonMapper json, Supplier<String> generadorIdEvento) {
        this.json = json;
        this.generadorIdEvento = generadorIdEvento;
    }

    /**
     * Traduce un evento de dominio a su mensaje de integración, si lo tiene.
     *
     * @param evento        evento de dominio
     * @param idCorrelacion correlación de la petición, o {@code null}
     * @return el mensaje, o vacío si el evento no sale del servicio
     */
    public Optional<MensajeIntegracion> traducir(EventoDeDominio evento, String idCorrelacion) {
        return switch (evento) {
            case PreguntaPublicada publicada -> Optional.of(mensaje(RUTA_PREGUNTA_PUBLICADA, "PreguntaPublicada", evento,
                    idCorrelacion, aDatos(publicada)));
            case PreguntaArchivada archivada -> Optional.of(mensaje(RUTA_PREGUNTA_ARCHIVADA, "PreguntaArchivada", evento,
                    idCorrelacion, new DatosPreguntaArchivada(archivada.preguntaId().toString(), archivada.motivo(),
                            archivada.fechaArchivado())));
            default -> Optional.empty();
        };
    }

    /**
     * Construye el sobre de 7.3 sin serializarlo (útil para las pruebas del contrato).
     *
     * @param tipoEvento    nombre del evento
     * @param evento        evento de dominio
     * @param idCorrelacion correlación o {@code null}
     * @param datos         datos propios del evento
     * @param <T>           tipo de los datos
     * @return el sobre
     */
    public <T> SobreEvento<T> sobre(String tipoEvento, EventoDeDominio evento, String idCorrelacion, T datos) {
        return new SobreEvento<>(generadorIdEvento.get(), tipoEvento, VERSION_EVENTO, evento.fechaOcurrencia(), ORIGEN,
                idCorrelacion, datos);
    }

    private MensajeIntegracion mensaje(String ruta, String tipoEvento, EventoDeDominio evento, String idCorrelacion,
                                       Object datos) {
        SobreEvento<Object> sobre = sobre(tipoEvento, evento, idCorrelacion, datos);
        return new MensajeIntegracion(ruta, sobre.idEvento(), tipoEvento, json.writeValueAsBytes(sobre));
    }

    // CONTRATOS.md 7.4: solo lo que Evaluación necesita (sin autor, justificación, bibliografía ni revisores).
    private static DatosPreguntaPublicada aDatos(PreguntaPublicada evento) {
        ClasificacionAcademica clasificacion = evento.clasificacion();
        return new DatosPreguntaPublicada(
                evento.preguntaId().toString(),
                evento.contexto(),
                evento.preguntaDirecta(),
                evento.opciones().stream()
                        .map(opcion -> new DatosPreguntaPublicada.Opcion(opcion.letra(), opcion.texto()))
                        .toList(),
                evento.letraCorrecta(),
                new DatosPreguntaPublicada.Clasificacion(clasificacion.competenciaId().toString(),
                        clasificacion.temaId().toString(), clasificacion.subtemaId().toString()),
                evento.nivelDificultad().name(),
                evento.fechaPublicacion());
    }
}
