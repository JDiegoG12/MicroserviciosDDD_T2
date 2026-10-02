import { Schema } from 'mongoose';

/**
 * Forma del documento de Mongo para la coleccion `intentos`
 * (CONTRATOS.md 11.3). El `_id` es el `intentoId` en texto.
 */
export interface IntentoDeSimulacroDocumento {
  readonly _id: string;
  readonly simulacroId: string;
  readonly estudianteId: string;
  readonly preguntasDelSimulacro: readonly { preguntaId: string; posicion: number }[];
  readonly fechaInicio: Date;
  readonly fechaLimite: Date;
  readonly estado: string;
  readonly fechaFinalizacion: Date | null;
  readonly finalizadoPor: string | null;
  readonly respuestas: readonly { preguntaId: string; letraSeleccionada: string }[];
  readonly calificacion: {
    readonly totalPreguntas: number;
    readonly correctas: number;
    readonly puntaje: number;
    readonly desglosePorCompetencia: readonly {
      competenciaId: string;
      totalPreguntas: number;
      correctas: number;
      puntaje: number;
    }[];
  } | null;
}

export const esquemaIntentoDeSimulacro = new Schema<IntentoDeSimulacroDocumento>(
  {
    _id: { type: String },
    simulacroId: { type: String, required: true },
    estudianteId: { type: String, required: true },
    preguntasDelSimulacro: {
      type: [new Schema({ preguntaId: String, posicion: Number }, { _id: false })],
      required: true,
    },
    fechaInicio: { type: Date, required: true },
    fechaLimite: { type: Date, required: true },
    estado: { type: String, required: true },
    fechaFinalizacion: { type: Date, default: null },
    finalizadoPor: { type: String, default: null },
    respuestas: {
      type: [new Schema({ preguntaId: String, letraSeleccionada: String }, { _id: false })],
      default: [],
    },
    calificacion: {
      type: new Schema(
        {
          totalPreguntas: Number,
          correctas: Number,
          puntaje: Number,
          desglosePorCompetencia: {
            type: [
              new Schema(
                { competenciaId: String, totalPreguntas: Number, correctas: Number, puntaje: Number },
                { _id: false },
              ),
            ],
          },
        },
        { _id: false },
      ),
      default: null,
    },
  },
  { collection: 'intentos', versionKey: false },
);

// Indices del detalle de servicio-evaluacion (etapa 2).
esquemaIntentoDeSimulacro.index({ estudianteId: 1 });
esquemaIntentoDeSimulacro.index({ simulacroId: 1 });
