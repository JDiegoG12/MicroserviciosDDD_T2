import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { ResultadoPaginado } from '../../../dominio/compartido/resultado-paginado';
import { PreguntaEvaluable } from '../../../dominio/preguntas-evaluables/pregunta-evaluable';
import {
  FiltrosPreguntaEvaluable,
  PreguntaEvaluableRepositorio,
} from '../../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { CriterioDeGeneracion } from '../../../dominio/simulacros/criterio-de-generacion';
import { aDocumentoPreguntaEvaluable, aPreguntaEvaluable } from './pregunta-evaluable.mapper';
import { PreguntaEvaluableDocumento } from './pregunta-evaluable.schema';
import { verificarConexionMongo } from './verificar-conexion-mongo';

/**
 * Implementacion de `PreguntaEvaluableRepositorio` sobre MongoDB (coleccion
 * `preguntas_evaluables`, CONTRATOS.md 11.3).
 *
 * `buscarPublicadasPorCriterios` traduce la semantica de CONTRATOS.md 8.3
 * a una consulta de Mongo: `$in` dentro de cada lista (O), combinacion
 * entre listas como campos distintos del mismo filtro (Y), y una lista
 * vacia se omite del filtro (no filtra).
 */
@Injectable()
export class PreguntaEvaluableRepositorioMongo implements PreguntaEvaluableRepositorio {
  constructor(
    @InjectModel('PreguntaEvaluable') private readonly modelo: Model<PreguntaEvaluableDocumento>,
  ) {}

  public async guardar(pregunta: PreguntaEvaluable): Promise<void> {
    verificarConexionMongo(this.modelo.db);
    const documento = aDocumentoPreguntaEvaluable(pregunta);
    // Idempotencia (CONTRATOS.md 7.7.2): upsert por preguntaId (_id).
    await this.modelo
      .findByIdAndUpdate(documento._id, documento, { upsert: true, setDefaultsOnInsert: true })
      .exec();
  }

  public async obtenerPorId(preguntaId: string): Promise<PreguntaEvaluable | null> {
    verificarConexionMongo(this.modelo.db);
    const documento = await this.modelo.findById(preguntaId).lean().exec();
    return documento ? aPreguntaEvaluable(documento) : null;
  }

  public async obtenerPorIds(preguntaIds: readonly string[]): Promise<PreguntaEvaluable[]> {
    verificarConexionMongo(this.modelo.db);
    if (preguntaIds.length === 0) {
      return [];
    }
    const documentos = await this.modelo
      .find({ _id: { $in: [...preguntaIds] } })
      .lean()
      .exec();
    return documentos.map(aPreguntaEvaluable);
  }

  public async buscarPublicadasPorCriterios(criterio: CriterioDeGeneracion): Promise<PreguntaEvaluable[]> {
    verificarConexionMongo(this.modelo.db);
    const filtro: Record<string, unknown> = { estado: 'PUBLICADA' };
    if (criterio.competenciaIds.length > 0) {
      filtro['contenido.clasificacion.competenciaId'] = { $in: [...criterio.competenciaIds] };
    }
    if (criterio.temaIds.length > 0) {
      filtro['contenido.clasificacion.temaId'] = { $in: [...criterio.temaIds] };
    }
    if (criterio.subtemaIds.length > 0) {
      filtro['contenido.clasificacion.subtemaId'] = { $in: [...criterio.subtemaIds] };
    }
    if (criterio.nivelesDificultad.length > 0) {
      filtro['contenido.nivelDificultad'] = { $in: [...criterio.nivelesDificultad] };
    }
    const documentos = await this.modelo.find(filtro).lean().exec();
    return documentos.map(aPreguntaEvaluable);
  }

  public async buscarPaginado(
    filtros: FiltrosPreguntaEvaluable,
    pagina: number,
    tamano: number,
  ): Promise<ResultadoPaginado<PreguntaEvaluable>> {
    verificarConexionMongo(this.modelo.db);
    const filtro: Record<string, unknown> = {};
    if (filtros.estado) {
      filtro.estado = filtros.estado;
    }
    if (filtros.nivelDificultad) {
      filtro['contenido.nivelDificultad'] = filtros.nivelDificultad;
    }
    if (filtros.competenciaId) {
      filtro['contenido.clasificacion.competenciaId'] = filtros.competenciaId;
    }

    const [documentos, totalElementos] = await Promise.all([
      this.modelo
        .find(filtro)
        .sort({ fechaActualizacion: -1 })
        .skip(pagina * tamano)
        .limit(tamano)
        .lean()
        .exec(),
      this.modelo.countDocuments(filtro).exec(),
    ]);

    return { elementos: documentos.map(aPreguntaEvaluable), totalElementos };
  }
}
