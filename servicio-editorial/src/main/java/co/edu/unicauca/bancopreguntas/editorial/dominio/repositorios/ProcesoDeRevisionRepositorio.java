package co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio del agregado Proceso de revisión, con sus Asignaciones y Formatos de evaluación
 * (Taller 1, sección 12.2).
 */
public interface ProcesoDeRevisionRepositorio {

    /**
     * Guarda el Proceso completo (alta o actualización).
     *
     * @param proceso agregado a guardar
     */
    void guardar(ProcesoDeRevision proceso);

    /**
     * Busca un Proceso por su identidad.
     *
     * @param procesoId identidad
     * @return el Proceso o vacío si no existe
     */
    Optional<ProcesoDeRevision> obtenerPorId(ProcesoDeRevisionId procesoId);

    /**
     * Devuelve el Proceso vigente de una Pregunta: el abierto o, si no hay, el último cerrado (Taller 1, 12.2).
     *
     * @param preguntaId Pregunta
     * @return el Proceso vigente o vacío si la Pregunta nunca tuvo uno
     */
    Optional<ProcesoDeRevision> obtenerVigentePorPregunta(PreguntaId preguntaId);

    /**
     * Devuelve todos los Procesos de una Pregunta, del más antiguo al más reciente (CU-18; D-07).
     *
     * @param preguntaId Pregunta
     * @return Procesos en orden de apertura
     */
    List<ProcesoDeRevision> buscarHistoricosPorPregunta(PreguntaId preguntaId);

    /**
     * Devuelve los Procesos {@code ABIERTO} en los que un Revisor tiene Asignación (CU-06; Taller 1, 12.2).
     *
     * @param revisorId Revisor
     * @return Procesos abiertos donde está asignado
     */
    List<ProcesoDeRevision> buscarActivosPorRevisor(UsuarioId revisorId);

    /**
     * Consulta paginada de Procesos por estado y, opcionalmente, por Revisor asignado (CU-06; CONTRATOS.md 8.1,
     * {@code GET /procesos-revision}). Orden estable: fecha de apertura y luego identificador.
     *
     * @param estado     estado de los Procesos
     * @param revisorId  Revisor asignado, o {@code null} para todos los Procesos de ese estado
     * @param paginacion página solicitada
     * @return página de Procesos
     */
    Pagina<ProcesoDeRevision> buscarPorEstadoYRevisor(EstadoProceso estado, UsuarioId revisorId, Paginacion paginacion);
}
