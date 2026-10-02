package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Formas de UUID aceptadas en la entrada (CONTRATOS.md 4, "Identificadores", v1.9).
 */
@DisplayName("Validaciones.aUuid (CONTRATOS.md 4)")
class ValidacionesTest {

    @Test
    @DisplayName("La forma canónica se acepta; las mayúsculas se normalizan a minúsculas")
    void formaCanonica() {
        assertThat(Validaciones.aUuid("3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6c", "id").toString())
                .isEqualTo("3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6c");
        assertThat(Validaciones.aUuid("3F2B6C1E-8A4D-4C2E-9B1A-1D2E3F4A5B6C", "id").toString())
                .isEqualTo("3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6c");
    }

    @Test
    @DisplayName("No se exige la versión 4")
    void noExigeVersion4() {
        assertThat(Validaciones.aUuid("22222222-2222-1222-0222-000000000101", "id")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6c}",
            "urn:uuid:3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6c",
            "3f2b6c1e8a4d4c2e9b1a1d2e3f4a5b6c",
            "1-1-1-1-1",
            " 3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6c",
            "3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6g",
            ""})
    @DisplayName("Cualquier otra forma → SOLICITUD_INVALIDA")
    void otrasFormasSeRechazan(String texto) {
        lanzaConCodigo(() -> Validaciones.aUuid(texto, "id"), "SOLICITUD_INVALIDA");
    }
}
