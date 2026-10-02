import { Schema } from 'mongoose';

/**
 * Forma del documento de Mongo para la coleccion `eventos_procesados`
 * (CONTRATOS.md 7.7.2): la base de la idempotencia del consumidor. El
 * `_id` es el `idEvento` en texto, lo que ya garantiza su unicidad sin
 * necesitar un indice adicional (detalle de servicio-evaluacion, etapa 2:
 * "en eventos_procesados: idEvento unico").
 */
export interface EventoProcesadoDocumento {
  readonly _id: string;
  readonly fechaProcesado: Date;
}

export const esquemaEventoProcesado = new Schema<EventoProcesadoDocumento>(
  {
    _id: { type: String },
    fechaProcesado: { type: Date, required: true },
  },
  { collection: 'eventos_procesados', versionKey: false },
);
