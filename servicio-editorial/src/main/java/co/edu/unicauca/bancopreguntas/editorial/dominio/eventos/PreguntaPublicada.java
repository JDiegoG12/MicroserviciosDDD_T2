package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.NivelDeDificultad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;
import java.util.List;

/**
 * La Pregunta quedó {@code PUBLICADA} (CU-08). Lleva todos los datos que necesita el evento de
 * integración de CONTRATOS.md 7.4 y ninguno más: sin autor, justificación, bibliografía, revisores ni
 * trazabilidad.
 *
 * @param preguntaId       pregunta publicada
 * @param contexto         Contexto
 * @param preguntaDirecta  Pregunta directa
 * @param opciones         las cuatro opciones, en el orden definido por Editorial, sin la marca de correcta
 * @param letraCorrecta    letra de la Respuesta correcta (D-01)
 * @param clasificacion    clasificación académica (solo identificadores, D-13)
 * @param nivelDificultad  nivel de dificultad (D-10)
 * @param fechaPublicacion instante de la publicación; es también la fecha de ocurrencia
 */
public record PreguntaPublicada(
        PreguntaId preguntaId,
        String contexto,
        String preguntaDirecta,
        List<OpcionPublicada> opciones,
        String letraCorrecta,
        ClasificacionAcademica clasificacion,
        NivelDeDificultad nivelDificultad,
        Instant fechaPublicacion) implements EventoDeDominio {

    /**
     * Copia la lista de opciones para que el evento sea inmutable.
     */
    public PreguntaPublicada {
        opciones = List.copyOf(opciones);
    }

    /**
     * Instante del hecho: la fecha de publicación.
     *
     * @return fecha UTC
     */
    @Override
    public Instant fechaOcurrencia() {
        return fechaPublicacion;
    }

    /**
     * Opción tal como viaja en el evento 7.4: solo letra y texto.
     *
     * @param letra letra de la opción
     * @param texto texto de la opción
     */
    public record OpcionPublicada(String letra, String texto) {
    }
}
