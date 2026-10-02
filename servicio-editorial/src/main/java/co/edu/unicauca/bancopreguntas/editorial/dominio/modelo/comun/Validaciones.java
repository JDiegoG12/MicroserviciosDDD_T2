package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

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
     * Convierte un texto en UUID (CONTRATOS.md 4, "Identificadores", entradas): solo se acepta la forma canónica de
     * 36 caracteres con guiones; las mayúsculas se aceptan y se normalizan a minúsculas; no se exige la versión 4.
     * Cualquier otra forma (llaves, {@code urn:uuid:}, sin guiones, grupos cortos) se rechaza.
     *
     * @param texto  UUID en texto
     * @param nombre nombre del dato, para el mensaje
     * @return el UUID
     * @throws DatoInvalidoExcepcion si el texto es nulo o no tiene la forma canónica ({@code SOLICITUD_INVALIDA})
     */
    public static UUID aUuid(String texto, String nombre) {
        requerirNoNulo(texto, nombre);
        // UUID.fromString es tolerante (acepta "1-1-1-1-1"); por eso se exige primero la forma canónica.
        if (!UUID_CANONICO.matcher(texto).matches()) {
            throw new DatoInvalidoExcepcion("El dato '" + nombre + "' no es un UUID válido: " + texto);
        }
        return UUID.fromString(texto.toLowerCase(Locale.ROOT));
    }

    private static final Pattern UUID_CANONICO =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
}
