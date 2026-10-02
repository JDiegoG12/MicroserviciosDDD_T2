import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { IntentoDeSimulacro } from '../../../dominio/intentos/intento-de-simulacro';
import { IntentoDeSimulacroRepositorio } from '../../../dominio/intentos/intento-de-simulacro.repositorio';
import { IntentoId } from '../../../dominio/intentos/intento-id';
import { aDocumentoIntentoDeSimulacro, aIntentoDeSimulacro } from './intento-de-simulacro.mapper';
import { IntentoDeSimulacroDocumento } from './intento-de-simulacro.schema';
import { verificarConexionMongo } from './verificar-conexion-mongo';

/**
 * Implementacion de `IntentoDeSimulacroRepositorio` sobre MongoDB
 * (coleccion `intentos`, CONTRATOS.md 11.3).
 */
@Injectable()
export class IntentoDeSimulacroRepositorioMongo implements IntentoDeSimulacroRepositorio {
  constructor(
    @InjectModel('IntentoDeSimulacro') private readonly modelo: Model<IntentoDeSimulacroDocumento>,
  ) {}

  public async guardar(intento: IntentoDeSimulacro): Promise<void> {
    verificarConexionMongo(this.modelo.db);
    const documento = aDocumentoIntentoDeSimulacro(intento);
    await this.modelo
      .findByIdAndUpdate(documento._id, documento, { upsert: true, setDefaultsOnInsert: true })
      .exec();
  }

  public async obtenerPorId(intentoId: IntentoId): Promise<IntentoDeSimulacro | null> {
    verificarConexionMongo(this.modelo.db);
    const documento = await this.modelo.findById(intentoId.aTexto()).lean().exec();
    return documento ? aIntentoDeSimulacro(documento) : null;
  }
}
