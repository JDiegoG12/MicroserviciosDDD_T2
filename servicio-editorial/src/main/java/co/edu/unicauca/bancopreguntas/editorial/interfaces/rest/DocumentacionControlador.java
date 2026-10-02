package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /docs} sirve Swagger UI con un 200 (CONTRATOS.md 5.1), sin redirigir.
 *
 * <p>springdoc para Spring Boot 4 solo ofrece {@code /docs} como redirección (302) hacia
 * {@code /swagger-ui/index.html}. Esta página usa los mismos recursos estáticos de springdoc (rutas absolutas bajo
 * {@code /swagger-ui/}) y carga la especificación de {@code /openapi.json}.</p>
 */
@RestController
@Hidden
public class DocumentacionControlador {

    private static final String PAGINA = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <title>servicio-editorial · API REST</title>
              <link rel="stylesheet" href="/swagger-ui/swagger-ui.css">
              <link rel="icon" type="image/png" href="/swagger-ui/favicon-32x32.png">
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="/swagger-ui/swagger-ui-bundle.js"></script>
              <script src="/swagger-ui/swagger-ui-standalone-preset.js"></script>
              <script>
                window.ui = SwaggerUIBundle({
                  url: "/openapi.json",
                  dom_id: "#swagger-ui",
                  deepLinking: true,
                  presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                  layout: "StandaloneLayout"
                });
              </script>
            </body>
            </html>
            """;

    /**
     * Página de Swagger UI.
     *
     * @return HTML de Swagger UI apuntando a {@code /openapi.json}
     */
    @GetMapping(value = "/docs", produces = MediaType.TEXT_HTML_VALUE)
    public String documentacion() {
        return PAGINA;
    }
}
