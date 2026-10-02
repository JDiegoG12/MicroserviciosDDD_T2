package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * La Pregunta quedó {@code ARCHIVADA} (CU-09). Lleva los datos del evento de integración de
 * CONTRATOS.md 7.5.
 *
 * @param preguntaId     pregunta archivada
 * @param motivo         motivo obligatorio, de 1 a 500 caracteres
 * @param fechaArchivado instante del archivado; es también la fecha de ocurrencia
 */
public record PreguntaArchivada(PreguntaId preguntaId, String motivo, Instant fechaArchivado) implements EventoDeDominio {

    /**
     * Instante del hecho: la fecha de archivado.
     *
     * @return fecha UTC
     */
    @Override
    public Instant fechaOcurrencia() {
        return fechaArchivado;
    }
}
