import { EventoDeDominio } from './evento-de-dominio';

/**
 * Clase base de toda raiz de agregado del dominio.
 *
 * Acumula los eventos de dominio que sus propios metodos registran y los
 * entrega solo cuando la capa de aplicacion los pide, despues de guardar el
 * agregado (CLAUDE.md del servicio: "Los agregados acumulan sus eventos de
 * dominio en una lista interna. El caso de uso los extrae despues de guardar
 * el agregado y se los entrega a `PublicadorEventosPuerto`").
 */
export abstract class RaizDeAgregado {
  private readonly eventosPendientes: EventoDeDominio[] = [];

  /**
   * Agrega un evento a la lista interna. Solo las subclases (los agregados
   * concretos) pueden registrar eventos, como consecuencia de aplicar una de
   * sus propias reglas de negocio.
   *
   * @param evento Evento de dominio ya construido.
   */
  protected registrarEvento(evento: EventoDeDominio): void {
    this.eventosPendientes.push(evento);
  }

  /**
   * Devuelve los eventos acumulados y vacia la lista interna.
   *
   * @returns Los eventos pendientes, en el orden en que se registraron.
   */
  public extraerEventos(): EventoDeDominio[] {
    return this.eventosPendientes.splice(0, this.eventosPendientes.length);
  }
}
