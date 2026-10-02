package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Dictamen;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;

/**
 * El Proceso de revisión emitió su Dictamen y quedó cerrado (CU-12, INV-19 a INV-21).
 *
 * @param procesoId  proceso
 * @param preguntaId pregunta evaluada
 * @param dictamen   dictamen emitido
 */
public record DictamenEmitido(ProcesoDeRevisionId procesoId, PreguntaId preguntaId, Dictamen dictamen)
        implements EventoDeDominio {

    /**
     * Instante del hecho: la fecha de emisión del dictamen.
     *
     * @return fecha UTC
     */
    @Override
    public Instant fechaOcurrencia() {
        return dictamen.fechaEmision();
    }
}
