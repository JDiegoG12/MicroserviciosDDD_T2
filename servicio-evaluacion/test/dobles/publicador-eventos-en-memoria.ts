import { EventoDeDominio } from '../../src/dominio/compartido/evento-de-dominio';
import { PublicadorEventosPuerto } from '../../src/aplicacion/puertos/salida/publicador-eventos.puerto';

/**
 * Doble en memoria de `PublicadorEventosPuerto`: acumula los eventos
 * recibidos para que las pruebas los inspeccionen. No es codigo de
 * produccion.
 */
export class PublicadorEventosEnMemoria implements PublicadorEventosPuerto {
  public readonly eventosPublicados: EventoDeDominio[] = [];

  public async publicar(eventos: readonly EventoDeDominio[]): Promise<void> {
    this.eventosPublicados.push(...eventos);
  }

  /**
   * Busca el primer evento publicado de un tipo dado.
   *
   * @param tipoEvento Nombre del evento, por ejemplo `"IntentoDeSimulacroCalificado"`.
   * @returns El evento encontrado, o `undefined` si no se publico ninguno.
   */
  public buscarPorTipo<T extends EventoDeDominio>(tipoEvento: string): T | undefined {
    return this.eventosPublicados.find((evento) => evento.tipoEvento === tipoEvento) as T | undefined;
  }
}
