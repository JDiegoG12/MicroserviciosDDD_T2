package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.UUID;

/**
 * Identidad de la Pregunta, estable durante todo su ciclo de vida (Taller 1, sección 5).
 *
 * @param valor UUID v4 generado por este servicio, dueño del agregado (CONTRATOS.md 4)
 */
public record PreguntaId(UUID valor) {

    /**
     * Valida que el identificador exista.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public PreguntaId {
        Validaciones.requerirNoNulo(valor, "preguntaId");
    }

    /**
     * Genera un identificador nuevo.
     *
     * @return identificador UUID v4 aleatorio
     */
    public static PreguntaId generar() {
        return new PreguntaId(UUID.randomUUID());
    }

    /**
     * Crea el identificador a partir de su texto.
     *
     * @param texto UUID en texto
     * @return el identificador
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si no es UUID
     */
    public static PreguntaId de(String texto) {
        return new PreguntaId(Validaciones.aUuid(texto, "preguntaId"));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
