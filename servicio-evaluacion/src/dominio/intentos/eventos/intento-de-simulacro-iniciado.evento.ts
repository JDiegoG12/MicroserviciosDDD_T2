import { EventoDeDominio } from '../../compartido/evento-de-dominio';

/**
 * Evento emitido por `IntentoDeSimulacro.iniciar(...)`
 * (MODELO-DOMINIO.md B.6 §11.4, CU-14). Es un evento interno a este
 * servicio (no viaja por RabbitMQ).
 */
export class IntentoDeSimulacroIniciadoEvento implements EventoDeDominio {
  public readonly tipoEvento = 'IntentoDeSimulacroIniciado' as const;

  constructor(
    public readonly fechaOcurrencia: Date,
    public readonly intentoId: string,
    public readonly simulacroId: string,
    public readonly estudianteId: string,
  ) {}
}
