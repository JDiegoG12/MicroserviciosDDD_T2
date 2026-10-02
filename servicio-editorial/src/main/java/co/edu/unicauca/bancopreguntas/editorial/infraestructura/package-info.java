/**
 * Adaptadores de salida (CONTRATOS.md 3.3). <strong>Vacío en la etapa 1; se implementa en la etapa 2.</strong>
 *
 * <p>Aquí irán:</p>
 * <ul>
 *   <li>{@code PreguntaRepositorioJpa} y {@code ProcesoDeRevisionRepositorioJpa}: implementan los
 *       repositorios de {@code dominio.repositorios} sobre PostgreSQL ({@code bd-editorial}), con las
 *       entidades {@code @Entity} y sus <em>mappers</em> (el dominio nunca lleva anotaciones JPA).</li>
 *   <li>{@code CatalogoAcademicoGrpcAdaptador}: implementa {@code CatalogoAcademicoPuerto} como cliente gRPC
 *       de {@code CatalogoAcademico.ValidarClasificacion}, con deadline de 2 s y conexión perezosa
 *       (CONTRATOS.md 6 y 9.3).</li>
 *   <li>{@code PublicadorEventosRabbitMqAdaptador}: implementa {@code PublicadorEventosPuerto}, traduce
 *       {@code PreguntaPublicada} y {@code PreguntaArchivada} al JSON de CONTRATOS.md 7.3 a 7.5 y publica en
 *       el exchange {@code editorial.eventos} después del commit.</li>
 *   <li>El reloj del sistema ({@code RelojPuerto} en UTC) y la configuración de Spring que cablea los casos de uso.</li>
 * </ul>
 */
package co.edu.unicauca.bancopreguntas.editorial.infraestructura;
