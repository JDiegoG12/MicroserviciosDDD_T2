/**
 * Contrato comun de todo caso de uso de este servicio (CLAUDE.md del
 * servicio: "un caso de uso = una clase").
 *
 * @typeParam TEntrada Tipo del comando o consulta de entrada.
 * @typeParam TResultado Tipo del DTO de resultado.
 */
export interface CasoUso<TEntrada, TResultado> {
  /**
   * Ejecuta el caso de uso.
   *
   * @param entrada Comando o consulta de entrada.
   * @returns El resultado de la operacion.
   */
  ejecutar(entrada: TEntrada): Promise<TResultado>;
}
