package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-08, Publicar pregunta ({@code POST /preguntas/{preguntaId}/publicacion}).
 */
public interface PublicarPregunta {

    /**
     * Publica la Pregunta aprobada y emite {@code PreguntaPublicada}.
     *
     * @param usuario    Administrador
     * @param preguntaId UUID de la Pregunta
     * @return la Pregunta publicada
     */
    PreguntaRespuesta ejecutar(UsuarioActual usuario, String preguntaId);
}
