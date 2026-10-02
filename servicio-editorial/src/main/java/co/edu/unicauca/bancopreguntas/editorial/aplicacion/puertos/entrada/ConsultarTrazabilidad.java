package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.TrazabilidadRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-18, Consultar trazabilidad ({@code GET /preguntas/{preguntaId}/trazabilidad}).
 */
public interface ConsultarTrazabilidad {

    /**
     * Devuelve la historia completa de la Pregunta en orden cronológico.
     *
     * @param usuario    Administrador
     * @param preguntaId UUID de la Pregunta
     * @return trazabilidad e historial de revisiones
     */
    TrazabilidadRespuesta ejecutar(UsuarioActual usuario, String preguntaId);
}
