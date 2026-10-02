package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Dictamen;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;

/**
 * Copia inmutable de un Dictamen dentro del Historial de la Pregunta (CU-12 paso 4, D-15).
 *
 * @param procesoId proceso que emitió el dictamen
 * @param dictamen  dictamen emitido
 */
public record DictamenEnHistorial(ProcesoDeRevisionId procesoId, Dictamen dictamen) implements EntradaDeHistorial {

    /**
     * Valida los datos.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta un dato
     */
    public DictamenEnHistorial {
        Validaciones.requerirNoNulo(procesoId, "procesoId");
        Validaciones.requerirNoNulo(dictamen, "dictamen");
    }

    /**
     * Instante de emisión del dictamen.
     *
     * @return fecha UTC
     */
    @Override
    public Instant fecha() {
        return dictamen.fechaEmision();
    }
}
