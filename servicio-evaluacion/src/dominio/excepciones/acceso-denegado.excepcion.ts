import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * El usuario actual no tiene el rol requerido o no es el dueno del recurso
 * sobre el que intenta operar (CONTRATOS.md 5.3, codigo `ACCESO_DENEGADO`,
 * 403). En este servicio protege, entre otras, la invariante INV-29
 * (MODELO-DOMINIO.md A.2): solo el estudiante dueno de un IntentoDeSimulacro
 * puede responder, finalizarlo o consultarlo.
 */
export class AccesoDenegadoExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.ACCESO_DENEGADO, mensaje);
  }
}
