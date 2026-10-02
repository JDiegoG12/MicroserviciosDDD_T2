/**
 * Puerto de salida para la idempotencia del consumo de eventos, con la
 * estrategia "reclamar y liberar si falla" de CONTRATOS.md 7.7.2:
 *
 * 1. se reclama el `idEvento` de forma atomica;
 * 2. si ya estaba reclamado, se hace `ack` sin trabajar (evento duplicado);
 * 3. si el trabajo posterior falla, se libera el reclamo para que el
 *    mensaje se pueda reprocesar despues sin perder datos.
 *
 * Usado por `RegistrarPreguntaPublicadaCasoUso` y
 * `RegistrarPreguntaArchivadaCasoUso`.
 */
export interface RegistroEventosProcesadosPuerto {
  /**
   * Reclama un `idEvento` de forma atomica: dos llamadas concurrentes con
   * el mismo `idEvento` deben resolver una en `true` (la que reclama) y la
   * otra en `false` (la que llega tarde), sin importar el orden.
   *
   * @param idEvento Id del evento a reclamar.
   * @returns `true` si este es quien reclamo el evento (es nuevo, hay que
   * procesarlo); `false` si ya estaba reclamado (es un duplicado).
   */
  reclamar(idEvento: string): Promise<boolean>;

  /**
   * Libera un reclamo previo, porque el trabajo posterior al reclamo
   * fallo: una entrega futura del mismo `idEvento` debe poder reclamarlo
   * de nuevo y reprocesarlo, sin perder datos (CONTRATOS.md 7.7.2).
   *
   * Nunca lanza: si liberar falla (por ejemplo, Mongo cae justo en ese
   * instante), la implementacion registra el `idEvento` en el log para
   * resolverlo a mano, pero no interrumpe el manejo del error original
   * que provoco la liberacion.
   *
   * @param idEvento Id del evento cuyo reclamo se libera.
   */
  liberar(idEvento: string): Promise<void>;
}
