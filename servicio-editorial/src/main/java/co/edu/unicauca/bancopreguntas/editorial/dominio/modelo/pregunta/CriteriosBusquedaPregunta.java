package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;

import java.util.UUID;

/**
 * Filtros de la consulta de preguntas (CU-06; CONTRATOS.md 8.1, {@code GET /preguntas}).
 *
 * <p>Un filtro {@code null} no restringe. Todos los filtros presentes se combinan con Y.</p>
 *
 * @param competenciaId   Competencia o {@code null}
 * @param temaId          Tema o {@code null}
 * @param subtemaId       Subtema o {@code null}
 * @param nivelDificultad Nivel de dificultad o {@code null}
 * @param estado          estado del ciclo de vida o {@code null}
 * @param autorId         Autor o {@code null}
 */
public record CriteriosBusquedaPregunta(
        UUID competenciaId,
        UUID temaId,
        UUID subtemaId,
        NivelDeDificultad nivelDificultad,
        EstadoPregunta estado,
        UsuarioId autorId) {

    /**
     * Criterios que no filtran nada.
     *
     * @return criterios vacíos
     */
    public static CriteriosBusquedaPregunta sinFiltros() {
        return new CriteriosBusquedaPregunta(null, null, null, null, null, null);
    }

    /**
     * Devuelve una copia con el autor fijado (restricción de CU-06 para el rol {@code AUTOR}).
     *
     * @param autor autor obligatorio
     * @return criterios nuevos
     */
    public CriteriosBusquedaPregunta conAutor(UsuarioId autor) {
        return new CriteriosBusquedaPregunta(competenciaId, temaId, subtemaId, nivelDificultad, estado, autor);
    }

    /**
     * Devuelve una copia con el estado fijado (restricción de CU-06 para el rol {@code DOCENTE}).
     *
     * @param estadoFijo estado obligatorio
     * @return criterios nuevos
     */
    public CriteriosBusquedaPregunta conEstado(EstadoPregunta estadoFijo) {
        return new CriteriosBusquedaPregunta(competenciaId, temaId, subtemaId, nivelDificultad, estadoFijo, autorId);
    }

    /**
     * Comprueba si una Pregunta cumple todos los filtros presentes.
     *
     * @param pregunta pregunta a evaluar
     * @return {@code true} si la cumple
     */
    public boolean seCumplenEn(Pregunta pregunta) {
        ClasificacionAcademica clasificacion = pregunta.getContenido().clasificacion();
        return coincide(competenciaId, clasificacion.competenciaId())
                && coincide(temaId, clasificacion.temaId())
                && coincide(subtemaId, clasificacion.subtemaId())
                && coincide(nivelDificultad, pregunta.getContenido().nivelDificultad())
                && coincide(estado, pregunta.getEstado())
                && coincide(autorId, pregunta.getAutorId());
    }

    private static boolean coincide(Object filtro, Object valor) {
        return filtro == null || filtro.equals(valor);
    }
}
