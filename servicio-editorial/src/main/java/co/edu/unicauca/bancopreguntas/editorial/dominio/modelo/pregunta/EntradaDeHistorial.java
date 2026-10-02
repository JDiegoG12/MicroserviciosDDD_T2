package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;

/**
 * Elemento del Historial de revisiones: una evaluación o un dictamen de algún Proceso (Taller 1,
 * sección 6; D-15).
 */
public sealed interface EntradaDeHistorial permits EvaluacionEnHistorial, DictamenEnHistorial {

    /**
     * Proceso de revisión del que proviene la entrada.
     *
     * @return identificador del proceso
     */
    ProcesoDeRevisionId procesoId();

    /**
     * Instante en que se emitió la evaluación o el dictamen.
     *
     * @return fecha UTC
     */
    Instant fecha();
}
