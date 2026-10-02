/**
 * MongoDB no esta conectado en este momento (CONTRATOS.md 5.3 y 9.3.6,
 * codigo `BASE_DE_DATOS_NO_DISPONIBLE`, 503).
 *
 * `BASE_DE_DATOS_NO_DISPONIBLE` es un codigo transversal de CONTRATOS.md
 * 5.3 (comun a los tres servicios si su base de datos no responde), no una
 * invariante de este dominio: por eso no vive en `CodigoError` (que solo
 * lista los codigos propios de Evaluacion), igual que `NoAutenticadoExcepcion`
 * en `interfaces/rest`. El filtro global de excepciones la traduce por su
 * `codigo`, sin necesitar conocer esta clase.
 */
export class BaseDeDatosNoDisponibleExcepcion extends Error {
  public readonly codigo = 'BASE_DE_DATOS_NO_DISPONIBLE' as const;

  constructor() {
    super('MongoDB no esta conectado en este momento.');
    this.name = 'BaseDeDatosNoDisponibleExcepcion';
  }
}
