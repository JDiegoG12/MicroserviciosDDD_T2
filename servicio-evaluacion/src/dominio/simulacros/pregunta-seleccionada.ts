/**
 * Referencia por identificador a una Pregunta Publicada que forma parte de
 * un Simulacro, junto con su posicion dentro de el (MODELO-DOMINIO.md,
 * Value Object "Pregunta Seleccionada", B.3 §6).
 *
 * La posicion empieza en 1 (CONTRATOS.md 8.3: "posicion empieza en 1").
 */
export interface PreguntaSeleccionada {
  readonly preguntaId: string;
  readonly posicion: number;
}
