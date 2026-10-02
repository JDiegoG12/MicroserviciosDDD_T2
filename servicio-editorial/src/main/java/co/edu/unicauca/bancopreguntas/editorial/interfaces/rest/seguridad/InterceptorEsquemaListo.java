package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.seguridad;

import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.EstadoDelEsquema;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores.BaseDeDatosNoDisponibleExcepcion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Mientras las migraciones no terminen, todo {@code /api/v1/**} responde 503 {@code BASE_DE_DATOS_NO_DISPONIBLE}
 * en {@code application/problem+json} (CONTRATOS.md 9.3.6, v1.10). {@code /salud}, {@code /docs} y
 * {@code /openapi.json} no pasan por aquí y siguen en 200.
 */
public class InterceptorEsquemaListo implements HandlerInterceptor {

    private final EstadoDelEsquema estadoDelEsquema;

    /**
     * Crea el interceptor.
     *
     * @param estadoDelEsquema estado de las migraciones
     */
    public InterceptorEsquemaListo(EstadoDelEsquema estadoDelEsquema) {
        this.estadoDelEsquema = estadoDelEsquema;
    }

    /**
     * Corta la petición si el esquema aún no está listo.
     *
     * @param peticion  petición HTTP
     * @param respuesta respuesta HTTP
     * @param manejador controlador destino
     * @return {@code true} si el esquema está listo
     * @throws BaseDeDatosNoDisponibleExcepcion si las migraciones no han terminado
     */
    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
        if (!estadoDelEsquema.esquemaListo()) {
            throw new BaseDeDatosNoDisponibleExcepcion(
                    "La base de datos no está disponible o sus migraciones aún no terminan. Intente de nuevo en unos segundos.");
        }
        return true;
    }
}
