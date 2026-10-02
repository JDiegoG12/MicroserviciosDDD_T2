package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-07, Someter pregunta a revisión ({@code POST /preguntas/{preguntaId}/envio-revision}).
 */
public interface EnviarPreguntaARevision {

    /**
     * Somete la Pregunta a revisión.
     *
     * @param usuario    Autor de la Pregunta
     * @param preguntaId UUID de la Pregunta
     * @return la Pregunta en {@code PENDIENTE_REVISION}
     */
    PreguntaRespuesta ejecutar(UsuarioActual usuario, String preguntaId);
}
