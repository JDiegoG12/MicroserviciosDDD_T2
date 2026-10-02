/**
 * Puerto de salida que entrega la hora actual a los casos de uso.
 *
 * El dominio nunca consulta la hora del sistema por su cuenta (CLAUDE.md
 * del servicio): los casos de uso piden `ahora()` una sola vez al empezar
 * y la pasan como parametro a los metodos del dominio, para que las
 * pruebas sean deterministas. La implementacion real con `new Date()`
 * vive en `infraestructura` (etapa 2).
 */
export interface RelojPuerto {
  /**
   * Instante actual.
   *
   * @returns La fecha y hora actuales, en UTC.
   */
  ahora(): Date;
}
