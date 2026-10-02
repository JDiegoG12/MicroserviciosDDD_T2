package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;

/**
 * Puerto de entrada de CU-06 para Revisores ({@code GET /procesos-revision?revisorId={uuid}&estado=ABIERTO}).
 */
public interface ConsultarProcesosRevision {

    /**
     * Consulta los Procesos abiertos de un Revisor.
     *
     * @param usuario  quien consulta
     * @param consulta filtros y paginación
     * @return página de Procesos
     */
    Pagina<ProcesoRevisionRespuesta> ejecutar(UsuarioActual usuario, ConsultarProcesosConsulta consulta);
}
