/**
 * Comando de entrada de `RegistrarPreguntaArchivadaCasoUso`, construido por
 * el consumidor de RabbitMQ a partir del evento `PreguntaArchivada`
 * (CONTRATOS.md 7.5).
 *
 * `motivo` y `fechaArchivado` se persisten en `PreguntaEvaluable`
 * (`motivoArchivo`, `fechaArchivado`) y se exponen en
 * `GET /preguntas-evaluables` (CONTRATOS.md 8.3).
 */
export interface RegistrarPreguntaArchivadaComando {
  readonly idEvento: string;
  readonly preguntaId: string;
  readonly motivo: string;
  readonly fechaArchivado: Date;
}
