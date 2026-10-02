/**
 * Adaptadores de salida (CONTRATOS.md 3.3).
 *
 * <ul>
 *   <li>{@code persistencia}: entidades JPA, mappers y los repositorios {@code PreguntaRepositorioJpa} y
 *       {@code ProcesoDeRevisionRepositorioJpa} sobre PostgreSQL ({@code bd-editorial}); el esquema lo crea Flyway.</li>
 *   <li>{@code catalogo}: {@code CatalogoAcademicoGrpcAdaptador}, cliente gRPC de {@code CatalogoAcademico} (6).</li>
 *   <li>{@code mensajeria}: {@code PublicadorEventosRabbitMqAdaptador} y el traductor al sobre de 7.3 a 7.5.</li>
 *   <li>{@code transaccion}: decorador transaccional de los casos de uso.</li>
 *   <li>{@code correlacion}, {@code reloj} y {@code configuracion}: piezas transversales y cableado de Spring.</li>
 * </ul>
 */
package co.edu.unicauca.bancopreguntas.editorial.infraestructura;
