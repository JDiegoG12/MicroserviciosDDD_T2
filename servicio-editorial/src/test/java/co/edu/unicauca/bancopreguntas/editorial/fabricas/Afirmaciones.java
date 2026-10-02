package co.edu.unicauca.bancopreguntas.editorial.fabricas;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ExcepcionDeAplicacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.ExcepcionDeDominio;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Afirmaciones de prueba sobre los códigos de contrato de las excepciones (CONTRATOS.md 5.3).
 */
public final class Afirmaciones {

    private Afirmaciones() {
    }

    /**
     * Comprueba que la acción lance una excepción de dominio o de aplicación con el código indicado.
     *
     * @param accion acción que debe fallar
     * @param codigo código de contrato esperado
     */
    public static void lanzaConCodigo(ThrowingCallable accion, String codigo) {
        Throwable error = catchThrowable(accion);
        assertThat(error).as("se esperaba una excepción con código %s", codigo).isNotNull();
        String codigoObtenido = switch (error) {
            case ExcepcionDeDominio deDominio -> deDominio.getCodigo();
            case ExcepcionDeAplicacion deAplicacion -> deAplicacion.getCodigo();
            default -> throw new AssertionError("Excepción sin código de contrato: " + error, error);
        };
        assertThat(codigoObtenido).isEqualTo(codigo);
    }
}
