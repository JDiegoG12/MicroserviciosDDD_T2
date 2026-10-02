/**
 * Adaptadores de entrada (CONTRATOS.md 3.3). <strong>Vacío en la etapa 1; se implementa en la etapa 2.</strong>
 *
 * <p>Aquí irán los controladores REST de CONTRATOS.md 8.1 ({@code PreguntaControlador},
 * {@code ProcesoRevisionControlador}), que validan la forma de la petición, arman el {@code UsuarioActual}
 * desde los encabezados {@code X-Usuario-Id} y {@code X-Roles}, llaman a un caso de uso y traducen la
 * respuesta. También el manejador de errores {@code application/problem+json} (CONTRATOS.md 5.2), que
 * traduce el {@code codigo} de cada excepción a su estado HTTP (5.3), y la documentación Swagger en
 * {@code /docs} y el endpoint {@code /salud}.</p>
 */
package co.edu.unicauca.bancopreguntas.editorial.interfaces;
