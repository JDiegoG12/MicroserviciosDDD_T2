import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { RegistroEventosProcesadosPuerto } from '../../../aplicacion/puertos/salida/registro-eventos-procesados.puerto';
import { registrar } from '../../observabilidad/registrador';
import { EventoProcesadoDocumento } from './evento-procesado.schema';
import { verificarConexionMongo } from './verificar-conexion-mongo';

/** Codigo de error de Mongo para una violacion de llave unica (duplicado). */
const CODIGO_MONGO_LLAVE_DUPLICADA = 11000;

/**
 * Implementacion de `RegistroEventosProcesadosPuerto` sobre MongoDB
 * (coleccion `eventos_procesados`), con la estrategia "reclamar y liberar
 * si falla" de CONTRATOS.md 7.7.2.
 *
 * `reclamar` inserta el documento de forma atomica: si dos entregas del
 * mismo mensaje llegan casi al mismo tiempo (el `prefetch` de 10 permite
 * varias en vuelo a la vez), solo la primera inserta con exito (`true`);
 * la segunda choca con la llave duplicada y ve `false` de inmediato, antes
 * de que la primera termine su trabajo.
 */
@Injectable()
export class RegistroEventosProcesadosRepositorioMongo implements RegistroEventosProcesadosPuerto {
  constructor(
    @InjectModel('EventoProcesado') private readonly modelo: Model<EventoProcesadoDocumento>,
  ) {}

  public async reclamar(idEvento: string): Promise<boolean> {
    verificarConexionMongo(this.modelo.db);
    try {
      await this.modelo.create({ _id: idEvento, fechaProcesado: new Date() });
      return true;
    } catch (error) {
      if (this.esLlaveDuplicada(error)) {
        return false;
      }
      throw error;
    }
  }

  public async liberar(idEvento: string): Promise<void> {
    try {
      verificarConexionMongo(this.modelo.db);
      await this.modelo.deleteOne({ _id: idEvento }).exec();
    } catch (error) {
      // CONTRATOS.md 7.7.2: si liberar tambien falla, se registra para
      // resolverlo a mano; nunca se relanza, para no tapar el error
      // original que provoco la liberacion.
      registrar('error', 'No se pudo liberar el reclamo de idempotencia; revisar a mano.', {
        idEvento,
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }

  private esLlaveDuplicada(error: unknown): boolean {
    return (
      typeof error === 'object' &&
      error !== null &&
      'code' in error &&
      (error as { code?: number }).code === CODIGO_MONGO_LLAVE_DUPLICADA
    );
  }
}
