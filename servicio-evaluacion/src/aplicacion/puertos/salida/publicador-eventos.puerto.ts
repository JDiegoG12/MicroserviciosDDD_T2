import { EventoDeDominio } from '../../../dominio/compartido/evento-de-dominio';

/**
 * Puerto de salida al que los casos de uso entregan los eventos de dominio
 * extraidos de un agregado despues de guardarlo (CLAUDE.md del servicio).
 *
 * La etapa 2 implementa este puerto con un adaptador de RabbitMQ que arma
 * el sobre comun de CONTRATOS.md 7.3 (`idEvento`, `origen`,
 * `versionEvento`, `idCorrelacion`) y publica en el exchange
 * `evaluacion.eventos` **solo** el evento `IntentoDeSimulacroCalificado`
 * (CONTRATOS.md 7.6): los demas eventos de este servicio
 * (`SimulacroDefinido`, `IntentoDeSimulacroIniciado`,
 * `IntentoDeSimulacroFinalizado`) son internos y no viajan por el broker.
 */
export interface PublicadorEventosPuerto {
  /**
   * Publica (o descarta, segun corresponda) los eventos de dominio
   * recibidos.
   *
   * @param eventos Eventos extraidos de un agregado con `extraerEventos()`.
   */
  publicar(eventos: readonly EventoDeDominio[]): Promise<void>;
}
