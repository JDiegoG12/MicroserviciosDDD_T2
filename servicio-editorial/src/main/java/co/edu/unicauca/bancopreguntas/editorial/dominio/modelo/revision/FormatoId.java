package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.UUID;

/**
 * Identidad local del Formato de evaluación dentro de su Proceso (Taller 1, sección 5).
 *
 * @param valor UUID del formato
 */
public record FormatoId(UUID valor) {

    /**
     * Valida que el identificador exista.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public FormatoId {
        Validaciones.requerirNoNulo(valor, "formatoId");
    }

    /**
     * Genera un identificador nuevo.
     *
     * @return identificador UUID v4 aleatorio
     */
    public static FormatoId generar() {
        return new FormatoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
