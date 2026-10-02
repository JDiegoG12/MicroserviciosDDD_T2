package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.List;
import java.util.Optional;

/**
 * Componentes editables de la Pregunta, agrupados para crearla o modificarla de una sola vez (CU-04, CU-05).
 *
 * <p>La clasificación y el nivel de dificultad son obligatorios desde el principio (CONTRATOS.md 8.1).
 * El resto puede venir incompleto: la Pregunta queda en {@code BORRADOR} (D-02).</p>
 *
 * @param contexto         Contexto de la Pregunta
 * @param preguntaDirecta  Pregunta directa
 * @param opciones         Opciones de respuesta (lista inmutable, puede tener cualquier tamaño)
 * @param justificacion    Justificación de la respuesta correcta
 * @param bibliografia     Bibliografía
 * @param clasificacion    Clasificación académica (solo identificadores, D-13)
 * @param nivelDificultad  Nivel de dificultad (D-10)
 */
public record ContenidoDePregunta(
        Contexto contexto,
        PreguntaDirecta preguntaDirecta,
        List<OpcionDeRespuesta> opciones,
        Justificacion justificacion,
        Bibliografia bibliografia,
        ClasificacionAcademica clasificacion,
        NivelDeDificultad nivelDificultad) {

    /**
     * Valida la presencia de cada componente y copia la lista de opciones.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta un componente
     */
    public ContenidoDePregunta {
        Validaciones.requerirNoNulo(contexto, "contexto");
        Validaciones.requerirNoNulo(preguntaDirecta, "preguntaDirecta");
        Validaciones.requerirNoNulo(opciones, "opciones");
        opciones.forEach(opcion -> Validaciones.requerirNoNulo(opcion, "opciones[]"));
        opciones = List.copyOf(opciones);
        Validaciones.requerirNoNulo(justificacion, "justificacion");
        Validaciones.requerirNoNulo(bibliografia, "bibliografia");
        Validaciones.requerirNoNulo(clasificacion, "clasificacion");
        Validaciones.requerirNoNulo(nivelDificultad, "nivelDificultad");
    }

    /**
     * Devuelve la opción marcada como correcta, si hay exactamente una.
     *
     * @return la Respuesta correcta, o vacío si no hay ninguna o hay varias (RF-11)
     */
    public Optional<OpcionDeRespuesta> respuestaCorrecta() {
        List<OpcionDeRespuesta> correctas = opciones.stream().filter(OpcionDeRespuesta::esCorrecta).toList();
        return correctas.size() == 1 ? Optional.of(correctas.get(0)) : Optional.empty();
    }
}
