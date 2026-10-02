package co.edu.unicauca.bancopreguntas.editorial.dominio.servicios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;
import java.util.List;

/**
 * Servicio de dominio que abre el Proceso de revisión y pasa la Pregunta a {@code EN_REVISION}
 * (Taller 1, sección 10.2; CU-10).
 *
 * <p>Ambas cosas son un solo hecho del negocio (D-14), así que las hace el mismo servicio. Sin servicio
 * de Identidad no se puede comprobar que los ids tengan el rol Revisor: es una limitación documentada
 * (MODELO-DOMINIO A.2, INV-16; CONTRATOS.md 11.1).</p>
 */
public final class AsignadorRevisoresServicio {

    /**
     * Abre el Proceso con los Revisores indicados y luego inicia la revisión de la Pregunta.
     *
     * <p>Primero se validan los Revisores (INV-15, INV-16, INV-17) y después la transición de la Pregunta
     * (INV-09); si algo falla no queda nada a medias, porque el caso de uso todavía no ha guardado.</p>
     *
     * @param pregunta        Pregunta en {@code PENDIENTE_REVISION}
     * @param procesoId       identidad del Proceso nuevo
     * @param revisoresIds    Revisores propuestos
     * @param administradorId Administrador que asigna
     * @param fecha           instante de la asignación
     * @return el Proceso abierto
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisoresInsuficientesExcepcion si hay menos de dos
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisorDuplicadoExcepcion       si se repite un Revisor
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AutorNoPuedeSerRevisorExcepcion si el Autor es Revisor
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion  si la Pregunta no está {@code PENDIENTE_REVISION}
     */
    public ProcesoDeRevision asignar(Pregunta pregunta, ProcesoDeRevisionId procesoId, List<UsuarioId> revisoresIds,
                                     UsuarioId administradorId, Instant fecha) {
        ProcesoDeRevision proceso = ProcesoDeRevision.abrir(procesoId, pregunta.getId(), pregunta.getAutorId(),
                revisoresIds, fecha);
        // D-14: la apertura del Proceso y el paso a EN_REVISION son el mismo hecho del negocio.
        pregunta.iniciarRevision(administradorId, fecha);
        return proceso;
    }
}
