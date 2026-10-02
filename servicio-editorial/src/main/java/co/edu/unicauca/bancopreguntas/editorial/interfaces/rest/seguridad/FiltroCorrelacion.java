package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.seguridad;

import co.edu.unicauca.bancopreguntas.editorial.infraestructura.correlacion.ContextoCorrelacion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identificador de correlación de cada petición (CONTRATOS.md 4.1): si llega {@code X-Id-Correlacion} se usa; si
 * no, se genera un UUID v4. Se guarda en el MDC (sale en todas las líneas de log y viaja a gRPC y a los eventos) y
 * se devuelve en el encabezado de la respuesta.
 *
 * <p>CONTRATOS 4.1 (v1.10): si llega con un formato inválido se genera uno nuevo (UUID v4) y se registra un aviso en
 * el log; nunca es error. Se exige además la versión 4 porque el {@code idCorrelacion} viaja en los eventos y su
 * esquema estricto (7.8) solo admite UUID v4.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroCorrelacion extends OncePerRequestFilter {

    /** Nombre exacto del encabezado (CONTRATOS.md 4.1). */
    public static final String ENCABEZADO = "X-Id-Correlacion";

    private static final Logger LOG = LoggerFactory.getLogger(FiltroCorrelacion.class);
    private static final Pattern UUID_V4 =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String recibido = peticion.getHeader(ENCABEZADO);
        String idCorrelacion = esUuidValido(recibido) ? recibido.toLowerCase() : UUID.randomUUID().toString();
        ContextoCorrelacion.establecer(idCorrelacion);
        if (recibido != null && !esUuidValido(recibido)) {
            LOG.warn("Encabezado {} inválido ({}); se generó uno nuevo.", ENCABEZADO, recibido);
        }
        respuesta.setHeader(ENCABEZADO, idCorrelacion);
        try {
            cadena.doFilter(peticion, respuesta);
        } finally {
            ContextoCorrelacion.limpiar();
        }
    }

    private static boolean esUuidValido(String valor) {
        return valor != null && UUID_V4.matcher(valor.toLowerCase()).matches();
    }
}
