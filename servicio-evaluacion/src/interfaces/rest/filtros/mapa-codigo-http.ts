/**
 * Mapa de todo `codigo` de dominio, de aplicacion o transversal que este
 * servicio puede lanzar, a su HTTP y titulo legible (CONTRATOS.md 5.3 y
 * 8.3). El filtro global de excepciones lo usa para traducir sin
 * ambiguedad.
 */
export const MAPA_CODIGO_A_HTTP: Record<string, number> = {
  // Transversales (CONTRATOS.md 5.3, 4.1).
  SOLICITUD_INVALIDA: 400,
  NO_AUTENTICADO: 401,
  ACCESO_DENEGADO: 403,
  METODO_NO_PERMITIDO: 405,
  TIPO_DE_CONTENIDO_NO_SOPORTADO: 415,
  ERROR_INTERNO: 500,
  BASE_DE_DATOS_NO_DISPONIBLE: 503,
  // Propios de servicio-evaluacion (CONTRATOS.md 8.3).
  SIMULACRO_NO_ENCONTRADO: 404,
  INTENTO_NO_ENCONTRADO: 404,
  INTENTO_FINALIZADO: 409,
  INTENTO_YA_CALIFICADO: 409,
  TRANSICION_NO_PERMITIDA: 409,
  PREGUNTA_NO_PERTENECE_AL_SIMULACRO: 422,
  PREGUNTAS_INSUFICIENTES: 422,
  PREGUNTA_NO_PUBLICADA: 422,
  PREGUNTA_DUPLICADA_EN_SIMULACRO: 422,
  DURACION_INVALIDA: 422,
};

/** Titulo legible por `codigo`, para el campo `title` de CONTRATOS.md 5.2. */
export const MAPA_CODIGO_A_TITULO: Record<string, string> = {
  SOLICITUD_INVALIDA: 'Solicitud invalida',
  NO_AUTENTICADO: 'Faltan los encabezados de identidad',
  ACCESO_DENEGADO: 'Acceso denegado',
  METODO_NO_PERMITIDO: 'Metodo no permitido',
  TIPO_DE_CONTENIDO_NO_SOPORTADO: 'Tipo de contenido no soportado',
  ERROR_INTERNO: 'Error interno',
  BASE_DE_DATOS_NO_DISPONIBLE: 'La base de datos no esta disponible',
  SIMULACRO_NO_ENCONTRADO: 'Simulacro no encontrado',
  INTENTO_NO_ENCONTRADO: 'Intento no encontrado',
  INTENTO_FINALIZADO: 'El intento ya esta finalizado',
  INTENTO_YA_CALIFICADO: 'El intento ya fue calificado',
  TRANSICION_NO_PERMITIDA: 'Transicion de estado no permitida',
  PREGUNTA_NO_PERTENECE_AL_SIMULACRO: 'La pregunta no pertenece al simulacro',
  PREGUNTAS_INSUFICIENTES: 'Preguntas insuficientes para el simulacro',
  PREGUNTA_NO_PUBLICADA: 'La pregunta no esta publicada',
  PREGUNTA_DUPLICADA_EN_SIMULACRO: 'Pregunta duplicada en el simulacro',
  DURACION_INVALIDA: 'Duracion maxima invalida',
};
