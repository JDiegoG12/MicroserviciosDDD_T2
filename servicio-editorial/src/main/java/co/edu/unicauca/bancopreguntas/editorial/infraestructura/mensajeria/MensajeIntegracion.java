package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

/**
 * Mensaje listo para enviar a RabbitMQ: el JSON del sobre (CONTRATOS.md 7.3) y los datos que van en las
 * propiedades AMQP (7.2).
 *
 * @param routingKey routing key en {@code editorial.eventos}
 * @param idEvento   {@code message_id} del mensaje
 * @param tipoEvento {@code type} del mensaje
 * @param cuerpo     JSON en UTF-8
 */
public record MensajeIntegracion(String routingKey, String idEvento, String tipoEvento, byte[] cuerpo) {
}
