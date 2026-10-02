package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ArchivarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-09, Archivar pregunta ({@code POST /preguntas/{preguntaId}/archivado}).
 */
public interface ArchivarPregunta {

    /**
     * Archiva la Pregunta publicada y emite {@code PreguntaArchivada} con el motivo.
     *
     * @param usuario Administrador
     * @param comando Pregunta y motivo
     * @return la Pregunta archivada
     */
    PreguntaRespuesta ejecutar(UsuarioActual usuario, ArchivarPreguntaComando comando);
}
