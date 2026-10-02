import { UsuarioActual } from '../../compartido/usuario-actual';

/**
 * Comando de entrada de `RegistrarRespuestaCasoUso` (CU-14), futuro cuerpo
 * de `PUT /intentos/{intentoId}/respuestas/{preguntaId}` (CONTRATOS.md 8.3).
 */
export interface RegistrarRespuestaComando {
  readonly usuario: UsuarioActual;
  readonly intentoId: string;
  readonly preguntaId: string;
  readonly letraSeleccionada: string;
}
