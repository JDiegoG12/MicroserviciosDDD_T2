package co.edu.unicauca.bancopreguntas.editorial.dominio.servicios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.DictamenEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EvaluacionEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Dictamen;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoDeEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;

import java.time.Instant;
import java.util.Optional;

/**
 * Servicio de dominio que traslada al agregado Pregunta lo que produce el Proceso de revisión
 * (Taller 1, sección 10.3; CU-11 y CU-12).
 *
 * <p>Ningún agregado muta a otro: el Dictamen se calcula en {@link ProcesoDeRevision}, pero el cambio de
 * estado pertenece a la máquina de estados de {@link Pregunta}, y el Historial vive en la Pregunta
 * (D-15). Este servicio coordina ambos.</p>
 */
public final class ResolutorDictamenServicio {

    /**
     * Copia la evaluación recién registrada al Historial de la Pregunta y, si el Proceso ya emitió su
     * Dictamen, lo anexa también y ejecuta {@code aprobar} o {@code rechazar}.
     *
     * @param proceso   Proceso donde se registró la evaluación
     * @param formato   Formato de evaluación recién registrado
     * @param pregunta  Pregunta evaluada ({@code EN_REVISION})
     * @param usuarioId usuario cuya acción disparó la resolución (el Revisor)
     * @param fecha     instante de la resolución
     * @throws IllegalArgumentException si el Proceso no corresponde a la Pregunta
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion si la Pregunta no está {@code EN_REVISION}
     */
    public void resolver(ProcesoDeRevision proceso, FormatoDeEvaluacion formato, Pregunta pregunta,
                         UsuarioId usuarioId, Instant fecha) {
        if (!proceso.getPreguntaId().equals(pregunta.getId())) {
            throw new IllegalArgumentException("El proceso " + proceso.getId() + " no evalúa la pregunta " + pregunta.getId() + ".");
        }
        // D-15: el Historial de revisiones vive en la Pregunta y se alimenta de los Formatos del Proceso.
        pregunta.registrarEnHistorial(EvaluacionEnHistorial.desde(proceso.getId(), formato));
        Optional<Dictamen> dictamen = proceso.getDictamen();
        if (dictamen.isEmpty()) {
            return;
        }
        // CU-12 paso 4: el Dictamen también queda en el Historial.
        pregunta.registrarEnHistorial(new DictamenEnHistorial(proceso.getId(), dictamen.get()));
        if (dictamen.get().esAprobatorio()) {
            pregunta.aprobar(usuarioId, fecha);
        } else {
            // D-07: la Pregunta rechazada vuelve a EN_CONSTRUCCION conservando su Historial.
            pregunta.rechazar(usuarioId, fecha);
        }
    }
}
