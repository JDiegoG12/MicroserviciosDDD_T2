package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.seguridad;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores.NoAutenticadoExcepcion;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Arma el {@link UsuarioActual} de cada petición a {@code /api/v1} desde los encabezados de identidad
 * (CONTRATOS.md 4.1):
 * <ul>
 *   <li>falta {@code X-Usuario-Id} o {@code X-Roles} (o vienen vacíos) → 401 {@code NO_AUTENTICADO};</li>
 *   <li>{@code X-Usuario-Id} no es un UUID → 400 {@code SOLICITUD_INVALIDA};</li>
 *   <li>{@code X-Roles} es una lista separada por comas; los espacios alrededor de cada rol no cuentan.</li>
 * </ul>
 *
 * <p>CONTRATOS 4.1 (v1.9): un rol que no es uno de los cinco válidos → 400 {@code SOLICITUD_INVALIDA}; {@code X-Roles}
 * vacío (o solo comas y espacios) → 401 {@code NO_AUTENTICADO}. {@code X-Usuario-Id} se acepta en mayúsculas y se
 * normaliza a minúsculas; cualquier forma no canónica → 400 (CONTRATOS 4).</p>
 */
@Component
public class ResolutorUsuarioActual implements HandlerMethodArgumentResolver {

    /** Encabezado con el UUID del usuario. */
    public static final String ENCABEZADO_USUARIO = "X-Usuario-Id";
    /** Encabezado con los roles del usuario. */
    public static final String ENCABEZADO_ROLES = "X-Roles";

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return UsuarioActual.class.equals(parametro.getParameterType());
    }

    @Override
    public UsuarioActual resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo, NativeWebRequest peticion,
                                         WebDataBinderFactory fabrica) {
        String usuario = peticion.getHeader(ENCABEZADO_USUARIO);
        String roles = peticion.getHeader(ENCABEZADO_ROLES);
        if (usuario == null || usuario.isBlank() || roles == null || roles.isBlank()) {
            throw new NoAutenticadoExcepcion("Faltan los encabezados " + ENCABEZADO_USUARIO + " y " + ENCABEZADO_ROLES + ".");
        }
        return new UsuarioActual(aUsuarioId(usuario.strip()), aRoles(roles));
    }

    private static UsuarioId aUsuarioId(String valor) {
        try {
            return UsuarioId.de(valor);
        } catch (DatoInvalidoExcepcion error) {
            throw new DatoInvalidoExcepcion("El encabezado " + ENCABEZADO_USUARIO + " debe ser un UUID.");
        }
    }

    private static Set<Rol> aRoles(String valor) {
        Set<Rol> roles = EnumSet.noneOf(Rol.class);
        Arrays.stream(valor.split(","))
                .map(String::strip)
                .filter(rol -> !rol.isEmpty())
                .forEach(rol -> roles.add(aRol(rol)));
        if (roles.isEmpty()) {
            throw new NoAutenticadoExcepcion("El encabezado " + ENCABEZADO_ROLES + " no trae ningún rol.");
        }
        return roles;
    }

    private static Rol aRol(String nombre) {
        try {
            return Rol.valueOf(nombre);
        } catch (IllegalArgumentException error) {
            throw new DatoInvalidoExcepcion("Rol desconocido en " + ENCABEZADO_ROLES + ": " + nombre
                    + ". Roles válidos: ADMINISTRADOR, AUTOR, REVISOR, DOCENTE, ESTUDIANTE.");
        }
    }
}
