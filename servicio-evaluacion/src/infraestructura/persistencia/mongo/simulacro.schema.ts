import { Schema } from 'mongoose';

/**
 * Forma del documento de Mongo para la coleccion `simulacros`
 * (CONTRATOS.md 11.3). El `_id` es el `simulacroId` en texto.
 */
export interface SimulacroDocumento {
  readonly _id: string;
  readonly docenteId: string;
  readonly nombre: string;
  readonly criterio: {
    readonly competenciaIds: readonly string[];
    readonly temaIds: readonly string[];
    readonly subtemaIds: readonly string[];
    readonly nivelesDificultad: readonly string[];
  };
  readonly duracionMaximaMinutos: number;
  readonly preguntas: readonly { preguntaId: string; posicion: number }[];
  readonly fechaCreacion: Date;
}

export const esquemaSimulacro = new Schema<SimulacroDocumento>(
  {
    _id: { type: String },
    docenteId: { type: String, required: true },
    nombre: { type: String, required: true },
    criterio: {
      competenciaIds: { type: [String], default: [] },
      temaIds: { type: [String], default: [] },
      subtemaIds: { type: [String], default: [] },
      nivelesDificultad: { type: [String], default: [] },
    },
    duracionMaximaMinutos: { type: Number, required: true },
    preguntas: {
      type: [new Schema({ preguntaId: String, posicion: Number }, { _id: false })],
      required: true,
    },
    fechaCreacion: { type: Date, required: true },
  },
  { collection: 'simulacros', versionKey: false },
);

esquemaSimulacro.index({ fechaCreacion: -1 });
