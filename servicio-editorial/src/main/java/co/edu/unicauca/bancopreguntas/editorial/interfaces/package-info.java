/**
 * Adaptadores de entrada (CONTRATOS.md 3.3): la API REST de la sección 8.1.
 *
 * <ul>
 *   <li>{@code rest}: {@code PreguntaControlador}, {@code ProcesoRevisionControlador} y {@code SaludControlador}.</li>
 *   <li>{@code rest.dto}: DTOs JSON propios de la API y su mapeador; nunca se exponen entidades del dominio ni JPA.</li>
 *   <li>{@code rest.seguridad}: identidad desde {@code X-Usuario-Id} y {@code X-Roles} y correlación desde
 *       {@code X-Id-Correlacion} (4.1).</li>
 *   <li>{@code rest.errores}: manejador global {@code application/problem+json} (5.2) con la tabla de 5.3.</li>
 *   <li>{@code rest.openapi}: Swagger UI en {@code /docs} y OpenAPI en {@code /openapi.json}.</li>
 * </ul>
 */
package co.edu.unicauca.bancopreguntas.editorial.interfaces;
