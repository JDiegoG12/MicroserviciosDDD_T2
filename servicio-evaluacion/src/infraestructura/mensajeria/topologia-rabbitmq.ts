import { ConfirmChannel } from 'amqplib';

/** Exchange de Editorial, declarado tambien por Evaluacion de forma idempotente (CONTRATOS.md 7.1). */
export const EXCHANGE_EDITORIAL_EVENTOS = 'editorial.eventos';

/** Dead-letter exchange de la cola de preguntas (CONTRATOS.md 7.1). */
export const EXCHANGE_EVALUACION_DLX = 'evaluacion.dlx';

/** Exchange de los eventos que publica este servicio (CONTRATOS.md 7.1). */
export const EXCHANGE_EVALUACION_EVENTOS = 'evaluacion.eventos';

/** Cola principal de consumo (CONTRATOS.md 7.1). */
export const COLA_EVALUACION_PREGUNTAS = 'evaluacion.preguntas';

/** Cola de mensajes muertos (CONTRATOS.md 7.1). */
export const COLA_EVALUACION_PREGUNTAS_DLQ = 'evaluacion.preguntas.dlq';

/** Routing key del evento `PreguntaPublicada` (CONTRATOS.md 7.4). */
export const ROUTING_KEY_PREGUNTA_PUBLICADA = 'pregunta.publicada';

/** Routing key del evento `PreguntaArchivada` (CONTRATOS.md 7.5). */
export const ROUTING_KEY_PREGUNTA_ARCHIVADA = 'pregunta.archivada';

/** Routing key del evento `IntentoDeSimulacroCalificado` (CONTRATOS.md 7.6). */
export const ROUTING_KEY_INTENTO_CALIFICADO = 'intento.calificado';

/** Cuantos mensajes sin confirmar puede tener el consumidor a la vez (detalle del servicio, etapa 2). */
export const PREFETCH_CONSUMIDOR = 10;

/**
 * Declara, de forma idempotente y con los mismos argumentos que Editorial,
 * la topologia exacta de CONTRATOS.md 7.1: el exchange de Editorial (que
 * Evaluacion tambien declara porque es quien liga la cola), el
 * dead-letter-exchange, la cola principal con su DLX, la cola de mensajes
 * muertos, los dos bindings de la cola principal y el binding de la DLQ, y
 * el exchange de los eventos que publica este servicio.
 *
 * Si dos servicios declaran el mismo recurso con argumentos distintos,
 * RabbitMQ responde `PRECONDITION_FAILED` y el canal se cierra (CONTRATOS.md
 * 7.1): por eso estos argumentos deben copiarse tal cual.
 *
 * @param canal Canal AMQP (de confirmacion o normal) ya conectado.
 */
export async function asegurarTopologiaRabbitMq(canal: ConfirmChannel): Promise<void> {
  await canal.assertExchange(EXCHANGE_EDITORIAL_EVENTOS, 'topic', { durable: true, autoDelete: false });
  await canal.assertExchange(EXCHANGE_EVALUACION_DLX, 'fanout', { durable: true });
  await canal.assertExchange(EXCHANGE_EVALUACION_EVENTOS, 'topic', { durable: true, autoDelete: false });

  await canal.assertQueue(COLA_EVALUACION_PREGUNTAS_DLQ, { durable: true });
  await canal.assertQueue(COLA_EVALUACION_PREGUNTAS, {
    durable: true,
    exclusive: false,
    autoDelete: false,
    arguments: { 'x-dead-letter-exchange': EXCHANGE_EVALUACION_DLX },
  });

  await canal.bindQueue(COLA_EVALUACION_PREGUNTAS, EXCHANGE_EDITORIAL_EVENTOS, ROUTING_KEY_PREGUNTA_PUBLICADA);
  await canal.bindQueue(COLA_EVALUACION_PREGUNTAS, EXCHANGE_EDITORIAL_EVENTOS, ROUTING_KEY_PREGUNTA_ARCHIVADA);
  await canal.bindQueue(COLA_EVALUACION_PREGUNTAS_DLQ, EXCHANGE_EVALUACION_DLX, '');
}
