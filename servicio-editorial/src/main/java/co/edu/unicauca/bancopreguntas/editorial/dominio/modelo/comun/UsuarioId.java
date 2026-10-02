package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

import java.util.UUID;

/**
 * Identificador de un Usuario cuya identidad vive en Identidad y Acceso.
 *
 * <p>Es una referencia por identificador a otro contexto (Taller 1, sección 7): el dominio editorial
 * modela al Autor y al Revisor solo con su UUID.</p>
 *
 * @param valor UUID del usuario (CONTRATOS.md 4.1, encabezado {@code X-Usuario-Id})
 */
public record UsuarioId(UUID valor) {

    /**
     * Valida que el identificador exista.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public UsuarioId {
        Validaciones.requerirNoNulo(valor, "usuarioId");
    }

    /**
     * Crea el identificador a partir de su texto UUID.
     *
     * @param texto UUID en texto
     * @return el identificador
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si el texto no es UUID
     */
    public static UsuarioId de(String texto) {
        return new UsuarioId(Validaciones.aUuid(texto, "usuarioId"));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
