package co.edu.unicauca.bancopreguntas.editorial.infraestructura.correlacion;

import org.slf4j.MDC;

import java.util.Optional;

/**
 * Acceso al identificador de correlación de la petición en curso (CONTRATOS.md 4.1, encabezado
 * {@code X-Id-Correlacion}).
 *
 * <p>Vive en el MDC de SLF4J con la clave {@value #CLAVE}, así sale en todas las líneas de log. El filtro REST lo
 * coloca al inicio de cada petición, y el cliente gRPC y el publicador de eventos lo leen para propagarlo.</p>
 */
public final class ContextoCorrelacion {

    /** Clave del MDC y nombre del campo en los logs. */
    public static final String CLAVE = "idCorrelacion";

    private ContextoCorrelacion() {
    }

    /**
     * Fija el identificador de correlación del hilo actual.
     *
     * @param idCorrelacion UUID en texto
     */
    public static void establecer(String idCorrelacion) {
        MDC.put(CLAVE, idCorrelacion);
    }

    /**
     * Devuelve el identificador de correlación del hilo actual, si lo hay.
     *
     * @return UUID en texto, o vacío fuera de una petición
     */
    public static Optional<String> actual() {
        return Optional.ofNullable(MDC.get(CLAVE));
    }

    /**
     * Quita el identificador del hilo actual al terminar la petición.
     */
    public static void limpiar() {
        MDC.remove(CLAVE);
    }
}
