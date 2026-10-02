import { Schema } from 'mongoose';

/**
 * Forma del documento de Mongo para la coleccion `preguntas_evaluables`
 * (CONTRATOS.md 11.3). El `_id` es el `preguntaId` en texto (nunca
 * `ObjectId`, CLAUDE.md del servicio).
 *
 * Es un modelo de persistencia, separado del dominio (CONTRATOS.md 3.3,
 * regla 2): `PreguntaEvaluableRepositorioMongo` lo traduce con un *mapper*.
 */
export interface PreguntaEvaluableDocumento {
  readonly _id: string;
  readonly contenido: {
    readonly contexto: string;
    readonly preguntaDirecta: string;
    readonly opciones: readonly { letra: string; texto: string }[];
    readonly letraCorrecta: string;
    readonly clasificacion: { competenciaId: string; temaId: string; subtemaId: string };
    readonly nivelDificultad: string;
    readonly fechaPublicacion: Date;
  } | null;
  readonly estado: string;
  readonly motivoArchivo: string | null;
  readonly fechaArchivado: Date | null;
  readonly fechaActualizacion: Date;
}

export const esquemaPreguntaEvaluable = new Schema<PreguntaEvaluableDocumento>(
  {
    _id: { type: String },
    contenido: {
      type: new Schema(
        {
          contexto: { type: String, required: true },
          preguntaDirecta: { type: String, required: true },
          opciones: {
            type: [new Schema({ letra: String, texto: String }, { _id: false })],
            required: true,
          },
          letraCorrecta: { type: String, required: true },
          clasificacion: {
            competenciaId: { type: String, required: true },
            temaId: { type: String, required: true },
            subtemaId: { type: String, required: true },
          },
          nivelDificultad: { type: String, required: true },
          fechaPublicacion: { type: Date, required: true },
        },
        { _id: false },
      ),
      default: null,
    },
    estado: { type: String, required: true },
    motivoArchivo: { type: String, default: null },
    fechaArchivado: { type: Date, default: null },
    fechaActualizacion: { type: Date, required: true },
  },
  { collection: 'preguntas_evaluables', versionKey: false },
);

// Indices de CONTRATOS.md 11.3 / detalle de servicio-evaluacion (etapa 2).
esquemaPreguntaEvaluable.index({ estado: 1 });
esquemaPreguntaEvaluable.index({ 'contenido.clasificacion.competenciaId': 1 });
esquemaPreguntaEvaluable.index({ 'contenido.clasificacion.temaId': 1 });
esquemaPreguntaEvaluable.index({ 'contenido.clasificacion.subtemaId': 1 });
esquemaPreguntaEvaluable.index({ 'contenido.nivelDificultad': 1 });
