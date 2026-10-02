/**
 * Puerto de salida (inyectado como dependencia, no como puerto de
 * aplicacion) que abstrae la fuente de aleatoriedad que usa
 * `EnsambladorSimulacroServicio` para elegir preguntas al azar.
 *
 * El dominio nunca llama a `Math.random()` directamente (CLAUDE.md del
 * servicio): eso haria que las pruebas de seleccion aleatoria no fueran
 * deterministas. La implementacion real con `Math.random()` vive en
 * `infraestructura` (etapa 2); las pruebas usan un doble fijo.
 */
export interface FuenteAleatoria {
  /**
   * Siguiente numero pseudoaleatorio de la secuencia.
   *
   * @returns Un numero en el intervalo [0, 1).
   */
  siguiente(): number;
}
