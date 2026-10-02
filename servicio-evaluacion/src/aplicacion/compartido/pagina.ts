import { SolicitudInvalidaExcepcion } from '../../dominio/excepciones/solicitud-invalida.excepcion';

/**
 * Forma de respuesta paginada exacta de CONTRATOS.md 5.1.
 *
 * @typeParam T Tipo de los elementos del contenido.
 */
export interface Pagina<T> {
  readonly contenido: readonly T[];
  readonly pagina: number;
  readonly tamano: number;
  readonly totalElementos: number;
  readonly totalPaginas: number;
}

/** Tamano de pagina por defecto (CONTRATOS.md 5.1). */
export const TAMANO_PAGINA_POR_DEFECTO = 20;

/** Tamano de pagina maximo permitido (CONTRATOS.md 5.1). */
export const TAMANO_PAGINA_MAXIMO = 100;

/**
 * Valida los parametros de paginacion de una solicitud.
 *
 * @param pagina Numero de pagina solicitado (desde 0). Por defecto 0.
 * @param tamano Tamano de pagina solicitado. Por defecto 20.
 * @returns Los valores ya validados.
 * @throws SolicitudInvalidaExcepcion Si `pagina` es negativa, o si
 * `tamano` no esta entre 1 y 100 (CONTRATOS.md 5.1: "Un tamano mayor que
 * 100, menor que 1 o una pagina negativa -> 400 SOLICITUD_INVALIDA; no se
 * recorta en silencio").
 */
export function validarPaginacion(
  pagina = 0,
  tamano = TAMANO_PAGINA_POR_DEFECTO,
): { pagina: number; tamano: number } {
  if (!Number.isInteger(pagina) || pagina < 0) {
    throw new SolicitudInvalidaExcepcion('El parametro pagina debe ser un entero mayor o igual a 0.');
  }
  if (!Number.isInteger(tamano) || tamano < 1 || tamano > TAMANO_PAGINA_MAXIMO) {
    throw new SolicitudInvalidaExcepcion(
      `El parametro tamano debe ser un entero entre 1 y ${TAMANO_PAGINA_MAXIMO}.`,
    );
  }
  return { pagina, tamano };
}

/**
 * Construye una `Pagina<T>` a partir de un contenido ya recortado y el
 * total de elementos que cumplen el filtro.
 *
 * @param contenido Elementos de esta pagina.
 * @param pagina Numero de pagina (desde 0).
 * @param tamano Tamano de pagina.
 * @param totalElementos Total de elementos que cumplen el filtro, sin paginar.
 * @returns La pagina con la forma exacta de CONTRATOS.md 5.1.
 */
export function construirPagina<T>(
  contenido: readonly T[],
  pagina: number,
  tamano: number,
  totalElementos: number,
): Pagina<T> {
  return {
    contenido,
    pagina,
    tamano,
    totalElementos,
    totalPaginas: Math.ceil(totalElementos / tamano),
  };
}
