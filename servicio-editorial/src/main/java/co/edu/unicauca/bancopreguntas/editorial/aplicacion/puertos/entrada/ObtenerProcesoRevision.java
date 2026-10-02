package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada para consultar un Proceso de revisión ({@code GET /procesos-revision/{procesoId}}).
 */
public interface ObtenerProcesoRevision {

    /**
     * Obtiene el Proceso si el usuario es Administrador o Revisor asignado.
     *
     * @param usuario   quien consulta
     * @param procesoId UUID del Proceso
     * @return el Proceso
     */
    ProcesoRevisionRespuesta ejecutar(UsuarioActual usuario, String procesoId);
}
