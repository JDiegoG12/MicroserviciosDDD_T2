package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;
import java.util.List;

/**
 * Se asignaron los Revisores de un Proceso (CU-10; Taller 1, tabla 11.2).
 *
 * @param procesoId       proceso
 * @param revisoresIds    Revisores asignados
 * @param fechaOcurrencia instante de la asignación
 */
public record RevisoresAsignados(ProcesoDeRevisionId procesoId, List<UsuarioId> revisoresIds,
                                 Instant fechaOcurrencia) implements EventoDeDominio {

    /**
     * Copia la lista de Revisores para que el evento sea inmutable.
     */
    public RevisoresAsignados {
        revisoresIds = List.copyOf(revisoresIds);
    }
}
