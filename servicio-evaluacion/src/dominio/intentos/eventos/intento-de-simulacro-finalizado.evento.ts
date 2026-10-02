import { EventoDeDominio } from '../../compartido/evento-de-dominio';

/**
 * Finalizador que cerro el IntentoDeSimulacro (CONTRATOS.md 7.6).
 */
export type FinalizadorIntento = 'ESTUDIANTE' | 'TIEMPO_AGOTADO';

/**
 * Evento emitido por `IntentoDeSimulacro.finalizar(...)` cuando el
 * estudiante cierra el intento o vence la duracion maxima
 * (MODELO-DOMINIO.md B.6 §11.4: "dispara la calificacion automatica").
 *
 * Es un evento interno a este servicio: en el Taller 1 original dispararia
 * a `CalificadorSimulacroServicio` de forma asincrona, pero en el Taller 2
 * (MODELO-DOMINIO.md A.3) la calificacion ocurre en el mismo caso de uso de
 * finalizacion, sin pasar por el broker.
 */
export class IntentoDeSimulacroFinalizadoEvento implements EventoDeDominio {
  public readonly tipoEvento = 'IntentoDeSimulacroFinalizado' as const;

  constructor(
    public readonly fechaOcurrencia: Date,
    public readonly intentoId: string,
    public readonly finalizadoPor: FinalizadorIntento,
  ) {}
}
