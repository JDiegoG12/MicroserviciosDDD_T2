import { generarUuid } from '../../dominio/compartido/uuid';

/**
 * Sobre comun de todo evento de integracion (CONTRATOS.md 7.3).
 *
 * @typeParam T Forma de `datos`, especifica de cada evento.
 */
export interface SobreEvento<T> {
  readonly idEvento: string;
  readonly tipoEvento: string;
  readonly versionEvento: 1;
  readonly fechaOcurrencia: string;
  readonly origen: string;
  readonly idCorrelacion: string | null;
  readonly datos: T;
}

/**
 * Construye el sobre comun de CONTRATOS.md 7.3 para un evento saliente.
 *
 * @param tipoEvento Nombre del evento, por ejemplo `"IntentoDeSimulacroCalificado"`.
 * @param fechaOcurrenciaIso Fecha ISO-8601 UTC en la que ocurrio el hecho en el dominio.
 * @param origen Servicio que produce el evento (`"servicio-evaluacion"`).
 * @param idCorrelacion Id de correlacion de la peticion que origino el hecho, o `null`.
 * @param datos Datos especificos del evento.
 * @returns El sobre completo, listo para serializar a JSON.
 */
export function construirSobreEvento<T>(
  tipoEvento: string,
  fechaOcurrenciaIso: string,
  origen: string,
  idCorrelacion: string | null,
  datos: T,
): SobreEvento<T> {
  return {
    idEvento: generarUuid(),
    tipoEvento,
    versionEvento: 1,
    fechaOcurrencia: fechaOcurrenciaIso,
    origen,
    idCorrelacion,
    datos,
  };
}
