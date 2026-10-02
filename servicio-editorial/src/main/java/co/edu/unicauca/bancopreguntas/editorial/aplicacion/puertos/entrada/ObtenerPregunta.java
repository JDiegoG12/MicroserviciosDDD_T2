package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-06 para una sola Pregunta ({@code GET /preguntas/{preguntaId}}).
 */
public interface ObtenerPregunta {

    /**
     * Obtiene una Pregunta si es visible para el rol del usuario.
     *
     * @param usuario    quien consulta
     * @param preguntaId UUID de la Pregunta
     * @return la Pregunta
     */
    PreguntaRespuesta ejecutar(UsuarioActual usuario, String preguntaId);
}
