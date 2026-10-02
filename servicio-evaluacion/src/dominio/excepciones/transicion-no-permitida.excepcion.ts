import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * Se intento una transicion de estado que el agregado no permite desde su
 * estado actual. En este servicio protege la parte de INV-32 que no tiene
 * un codigo propio: cerrar con calificacion un intento que todavia no esta
 * FINALIZADO (CONTRATOS.md 5.3, codigo `TRANSICION_NO_PERMITIDA`, 409).
 * Calificar dos veces el mismo intento usa, en cambio, el codigo dedicado
 * `INTENTO_YA_CALIFICADO` (ver `IntentoYaCalificadoExcepcion`,
 * CONTRATOS.md 8.3).
 */
export class TransicionNoPermitidaExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.TRANSICION_NO_PERMITIDA, mensaje);
  }
}
