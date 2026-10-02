/**
 * Resultado de procesar un evento de publicacion o archivado de una
 * Pregunta (CONTRATOS.md 7.7.2).
 *
 * `'DUPLICADO'` significa que el `idEvento` ya se habia procesado antes y
 * el caso de uso no hizo nada (idempotencia); `'REGISTRADO'` significa que
 * la copia local se creo o actualizo.
 */
export type ResultadoRegistroEvento = 'REGISTRADO' | 'DUPLICADO';
