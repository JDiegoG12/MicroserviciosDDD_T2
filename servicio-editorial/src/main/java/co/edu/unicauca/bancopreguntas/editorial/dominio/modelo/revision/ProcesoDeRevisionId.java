package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.UUID;

/**
 * Identidad del Proceso de revisión (Taller 1, sección 5).
 *
 * @param valor UUID v4 generado por este servicio
 */
public record ProcesoDeRevisionId(UUID valor) {

    /**
     * Valida que el identificador exista.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public ProcesoDeRevisionId {
        Validaciones.requerirNoNulo(valor, "procesoId");
    }

    /**
     * Genera un identificador nuevo.
     *
     * @return identificador UUID v4 aleatorio
     */
    public static ProcesoDeRevisionId generar() {
        return new ProcesoDeRevisionId(UUID.randomUUID());
    }

    /**
     * Crea el identificador a partir de su texto.
     *
     * @param texto UUID en texto
     * @return el identificador
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si no es UUID
     */
    public static ProcesoDeRevisionId de(String texto) {
        return new ProcesoDeRevisionId(Validaciones.aUuid(texto, "procesoId"));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
