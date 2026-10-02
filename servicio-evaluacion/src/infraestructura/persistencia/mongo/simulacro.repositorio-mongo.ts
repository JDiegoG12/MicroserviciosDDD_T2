import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Simulacro } from '../../../dominio/simulacros/simulacro';
import { SimulacroId } from '../../../dominio/simulacros/simulacro-id';
import { SimulacroRepositorio } from '../../../dominio/simulacros/simulacro.repositorio';
import { aDocumentoSimulacro, aSimulacro } from './simulacro.mapper';
import { SimulacroDocumento } from './simulacro.schema';
import { verificarConexionMongo } from './verificar-conexion-mongo';

/**
 * Implementacion de `SimulacroRepositorio` sobre MongoDB (coleccion
 * `simulacros`, CONTRATOS.md 11.3).
 */
@Injectable()
export class SimulacroRepositorioMongo implements SimulacroRepositorio {
  constructor(@InjectModel('Simulacro') private readonly modelo: Model<SimulacroDocumento>) {}

  public async guardar(simulacro: Simulacro): Promise<void> {
    verificarConexionMongo(this.modelo.db);
    const documento = aDocumentoSimulacro(simulacro);
    await this.modelo
      .findByIdAndUpdate(documento._id, documento, { upsert: true, setDefaultsOnInsert: true })
      .exec();
  }

  public async obtenerPorId(simulacroId: SimulacroId): Promise<Simulacro | null> {
    verificarConexionMongo(this.modelo.db);
    const documento = await this.modelo.findById(simulacroId.aTexto()).lean().exec();
    return documento ? aSimulacro(documento) : null;
  }

  public async listar(): Promise<Simulacro[]> {
    verificarConexionMongo(this.modelo.db);
    const documentos = await this.modelo.find().sort({ fechaCreacion: -1 }).lean().exec();
    return documentos.map(aSimulacro);
  }
}
