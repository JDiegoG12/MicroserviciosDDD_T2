package co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Identidad de quien invoca un caso de uso. En la etapa 2 la arma el adaptador REST desde los encabezados
 * {@code X-Usuario-Id} y {@code X-Roles} (CONTRATOS.md 4.1). Un usuario puede tener varios roles (D-08).
 *
 * <p>La verificación de <strong>rol</strong> se hace en la capa de aplicación con
 * {@link #exigirAlgunRol(Rol...)}; las reglas de <strong>propiedad</strong> viven en el dominio.</p>
 *
 * @param id    identificador del usuario
 * @param roles roles declarados (conjunto inmutable)
 */
public record UsuarioActual(UsuarioId id, Set<Rol> roles) {

    /**
     * Valida y copia los roles.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta el id o los roles
     */
    public UsuarioActual {
        Validaciones.requerirNoNulo(id, "X-Usuario-Id");
        Validaciones.requerirNoNulo(roles, "X-Roles");
        roles = roles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(roles));
    }

    /**
     * Crea el usuario con una lista de roles.
     *
     * @param id    identificador
     * @param roles roles del usuario
     * @return el usuario actual
     */
    public static UsuarioActual de(UsuarioId id, Rol... roles) {
        return new UsuarioActual(id, Set.of(roles));
    }

    /**
     * Indica si el usuario tiene un rol.
     *
     * @param rol rol buscado
     * @return {@code true} si lo tiene
     */
    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }

    /**
     * Exige que el usuario tenga al menos uno de los roles indicados (CONTRATOS.md 8.1, columna "Rol").
     *
     * @param permitidos roles que habilitan la operación
     * @throws AccesoDenegadoExcepcion si no tiene ninguno ({@code ACCESO_DENEGADO}, HTTP 403)
     */
    public void exigirAlgunRol(Rol... permitidos) {
        boolean tieneAlguno = Arrays.stream(permitidos).anyMatch(this::tieneRol);
        if (!tieneAlguno) {
            throw new AccesoDenegadoExcepcion("La operación requiere uno de los roles " + Arrays.toString(permitidos) + ".");
        }
    }
}
