package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;

import java.util.UUID;

/**
 * Comprobaciones pequeñas que comparten los value objects al construirse.
 */
public final class Validaciones {

    private Validaciones() {
    }

    /**
     * Exige que un valor no sea nulo.
     *
     * @param valor  valor recibido
     * @param nombre nombre del dato, para el mensaje
     * @param <T>    tipo del valor
     * @return el mismo valor, si no es nulo
     * @throws DatoInvalidoExcepcion si el valor es nulo ({@code SOLICITUD_INVALIDA})
     */
    public static <T> T requerirNoNulo(T valor, String nombre) {
        if (valor == null) {
            throw new DatoInvalidoExcepcion("El dato '" + nombre + "' es obligatorio.");
        }
        return valor;
    }

    /**
     * Convierte un texto en UUID (CONTRATOS.md 4: identificadores UUID en texto).
     *
     * @param texto  UUID en texto
     * @param nombre nombre del dato, para el mensaje
     * @return el UUID
     * @throws DatoInvalidoExcepcion si el texto es nulo o no es un UUID ({@code SOLICITUD_INVALIDA})
     */
    public static UUID aUuid(String texto, String nombre) {
        requerirNoNulo(texto, nombre);
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException error) {
            throw new DatoInvalidoExcepcion("El dato '" + nombre + "' no es un UUID válido: " + texto);
        }
    }
}
