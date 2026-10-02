import { LetraOpcion } from '../compartido/letra-opcion';

/**
 * Opcion de respuesta seleccionada por un estudiante para una pregunta
 * dentro de un Intento de Simulacro (MODELO-DOMINIO.md, Value Object
 * "Respuesta del Estudiante", B.3 §6).
 *
 * Se define enteramente por la combinacion de `preguntaId` y
 * `letraSeleccionada`: no tiene identidad propia. Si el estudiante cambia
 * de opinion, `IntentoDeSimulacro.registrarRespuesta` reemplaza esta
 * instancia por una nueva (INV-30), nunca la edita.
 */
export interface RespuestaDelEstudiante {
  readonly preguntaId: string;
  readonly letraSeleccionada: LetraOpcion;
}
