package co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados;

import java.time.Instant;
import java.util.List;

/**
 * Resultado con la forma de {@code PreguntaRespuesta} (CONTRATOS.md 8.1): los campos de la solicitud más
 * {@code preguntaId}, {@code autorId}, {@code estado}, {@code erroresValidacion}, {@code fechaCreacion} y
 * {@code fechaActualizacion}. Aún sin anotaciones JSON (etapa 1).
 *
 * @param preguntaId         UUID de la Pregunta
 * @param autorId            UUID del Autor
 * @param contexto           Contexto
 * @param preguntaDirecta    Pregunta directa
 * @param opciones           Opciones de respuesta
 * @param justificacion      Justificación
 * @param bibliografia       referencias
 * @param clasificacion      identificadores del Catálogo
 * @param nivelDificultad    nombre del nivel
 * @param estado             nombre del estado del ciclo de vida
 * @param erroresValidacion  reglas incumplidas; vacía si supera la validación
 * @param fechaCreacion      instante de creación
 * @param fechaActualizacion instante del último cambio
 */
public record PreguntaRespuesta(
        String preguntaId,
        String autorId,
        String contexto,
        String preguntaDirecta,
        List<OpcionRespuesta> opciones,
        String justificacion,
        List<String> bibliografia,
        ClasificacionRespuesta clasificacion,
        String nivelDificultad,
        String estado,
        List<ErrorValidacionRespuesta> erroresValidacion,
        Instant fechaCreacion,
        Instant fechaActualizacion) {

    /**
     * Opción de respuesta.
     *
     * @param letra      letra
     * @param texto      texto
     * @param esCorrecta si es la Respuesta correcta
     */
    public record OpcionRespuesta(String letra, String texto, boolean esCorrecta) {
    }

    /**
     * Regla de la validación estructural incumplida ({@code [{ "regla": "RF-10", "mensaje": "…" }]}).
     *
     * @param regla   identificador de la regla
     * @param mensaje explicación
     */
    public record ErrorValidacionRespuesta(String regla, String mensaje) {
    }
}
