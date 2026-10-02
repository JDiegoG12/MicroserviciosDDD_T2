package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /salud} (CONTRATOS.md 5.1): fuera de {@code /api/v1} y sin encabezados de identidad. Indica que el
 * proceso está vivo; responde 200 aunque la base de datos o el broker no estén disponibles (9.3.6).
 */
@RestController
@Tag(name = "Salud")
public class SaludControlador {

    /** Nombre del servicio en la respuesta (CONTRATOS.md 5.1). */
    public static final String NOMBRE_SERVICIO = "servicio-editorial";

    /**
     * Respuesta de salud.
     *
     * @param estado   siempre {@code OK}
     * @param servicio nombre del servicio
     */
    public record Salud(String estado, String servicio) {
    }

    /**
     * Comprueba que el proceso está vivo.
     *
     * @return {@code {"estado":"OK","servicio":"servicio-editorial"}}
     */
    @GetMapping(value = "/salud", produces = "application/json")
    @Operation(summary = "Salud del servicio", description = "Sin encabezados. 200 mientras el proceso esté vivo.")
    public Salud salud() {
        return new Salud("OK", NOMBRE_SERVICIO);
    }
}
