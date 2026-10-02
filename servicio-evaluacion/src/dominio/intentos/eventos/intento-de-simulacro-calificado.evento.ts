import { EventoDeDominio } from '../../compartido/evento-de-dominio';
import { Calificacion } from '../calificacion';
import { FinalizadorIntento } from './intento-de-simulacro-finalizado.evento';

/**
 * Evento emitido por `IntentoDeSimulacro.cerrarConCalificacion(...)`.
 *
 * Es el unico evento de este servicio que **si** viaja por RabbitMQ
 * (exchange `evaluacion.eventos`, routing key `intento.calificado`,
 * CONTRATOS.md 7.6). Lleva todos los campos de la seccion 7.6: la
 * traduccion al sobre de integracion (idEvento, origen, versionEvento,
 * idCorrelacion) ocurre en `infraestructura` (etapa 2), nunca aqui.
 */
export class IntentoDeSimulacroCalificadoEvento implements EventoDeDominio {
  public readonly tipoEvento = 'IntentoDeSimulacroCalificado' as const;

  constructor(
    public readonly fechaOcurrencia: Date,
    public readonly intentoId: string,
    public readonly simulacroId: string,
    public readonly estudianteId: string,
    public readonly fechaInicio: Date,
    public readonly fechaFinalizacion: Date,
    public readonly finalizadoPor: FinalizadorIntento,
    public readonly calificacion: Calificacion,
  ) {}
}
