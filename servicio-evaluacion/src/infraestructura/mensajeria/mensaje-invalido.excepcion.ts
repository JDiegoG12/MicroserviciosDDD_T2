/**
 * Un mensaje consumido de `evaluacion.preguntas` no cumple el contrato
 * minimo que el consumidor necesita (CONTRATOS.md 7.7.4): el JSON no se
 * pudo leer, `versionEvento` distinto de 1, `tipoEvento` desconocido, o
 * falta (o tiene tipo incorrecto) un campo obligatorio que el consumidor
 * usa.
 *
 * Senaliza al consumidor que debe hacer `nack` sin reencolar (va a la
 * DLQ `evaluacion.preguntas.dlq`).
 */
export class MensajeInvalidoExcepcion extends Error {
  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'MensajeInvalidoExcepcion';
  }
}
