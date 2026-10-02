package co.edu.unicauca.bancopreguntas.editorial.dominio.servicios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Regla de la validación estructural que la Pregunta incumple. No es un error HTTP: se devuelve en
 * {@code erroresValidacion} (CONTRATOS.md 8.1 y 11.1).
 *
 * @param regla   identificador de la regla ({@code RF-08} … {@code RF-13} o {@code INV-04})
 * @param mensaje explicación legible en español
 */
public record ErrorDeValidacion(String regla, String mensaje) {

    /**
     * Exige la regla y el mensaje.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta alguno
     */
    public ErrorDeValidacion {
        Validaciones.requerirNoNulo(regla, "regla");
        Validaciones.requerirNoNulo(mensaje, "mensaje");
    }
}
