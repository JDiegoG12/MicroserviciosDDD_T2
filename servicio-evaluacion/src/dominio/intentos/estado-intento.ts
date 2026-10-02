/**
 * Estado de un IntentoDeSimulacro (CONTRATOS.md 11.3).
 *
 * ```
 * EN_CURSO --(el estudiante finaliza o vence la duracion)--> FINALIZADO
 * FINALIZADO --(CalificadorSimulacroServicio)--> CALIFICADO
 * ```
 */
export enum EstadoIntento {
  EN_CURSO = 'EN_CURSO',
  FINALIZADO = 'FINALIZADO',
  CALIFICADO = 'CALIFICADO',
}
