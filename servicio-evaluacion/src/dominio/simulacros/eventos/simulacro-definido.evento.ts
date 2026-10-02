import { EventoDeDominio } from '../../compartido/evento-de-dominio';

/**
 * Evento emitido por `Simulacro.definir(...)` cuando se ensambla un
 * Simulacro nuevo (MODELO-DOMINIO.md B.6 §11.3: "habilita a los Estudiantes
 * a presentar el Simulacro").
 *
 * Es un evento interno a este servicio (no viaja por RabbitMQ): hoy no
 * tiene consumidor fuera del propio bounded context.
 */
export class SimulacroDefinidoEvento implements EventoDeDominio {
  public readonly tipoEvento = 'SimulacroDefinido' as const;

  constructor(
    public readonly fechaOcurrencia: Date,
    public readonly simulacroId: string,
    public readonly docenteId: string,
  ) {}
}
