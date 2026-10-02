/**
 * Codigos de error exactos de CONTRATOS.md (secciones 5.3 y 8.3) que el
 * dominio y la aplicacion pueden lanzar. La etapa 2 los traduce al HTTP
 * correspondiente sin ambiguedad (CLAUDE.md del servicio).
 */
export enum CodigoError {
  /** 400: la solicitud esta mal formada o viola una regla de forma simple. */
  SOLICITUD_INVALIDA = 'SOLICITUD_INVALIDA',

  /** 403: el usuario no tiene el rol requerido o no es el dueno del recurso. */
  ACCESO_DENEGADO = 'ACCESO_DENEGADO',

  /** 404: el Simulacro solicitado no existe. */
  SIMULACRO_NO_ENCONTRADO = 'SIMULACRO_NO_ENCONTRADO',

  /** 404: el IntentoDeSimulacro solicitado no existe. */
  INTENTO_NO_ENCONTRADO = 'INTENTO_NO_ENCONTRADO',

  /** 409: el Intento ya no esta EN_CURSO (vencimiento perezoso, INV-31). */
  INTENTO_FINALIZADO = 'INTENTO_FINALIZADO',

  /** 422: la pregunta de la respuesta no pertenece al Simulacro del intento (INV-29). */
  PREGUNTA_NO_PERTENECE_AL_SIMULACRO = 'PREGUNTA_NO_PERTENECE_AL_SIMULACRO',

  /** 422: no hay suficientes Preguntas Publicadas que cumplan el criterio (INV-26). */
  PREGUNTAS_INSUFICIENTES = 'PREGUNTAS_INSUFICIENTES',

  /** 422: se intento incluir una pregunta que no esta PUBLICADA al definir un simulacro (INV-25, CONTRATOS.md 8.3). */
  PREGUNTA_NO_PUBLICADA = 'PREGUNTA_NO_PUBLICADA',

  /** 422: una misma pregunta aparece repetida dentro del mismo simulacro (INV-27, CONTRATOS.md 8.3). */
  PREGUNTA_DUPLICADA_EN_SIMULACRO = 'PREGUNTA_DUPLICADA_EN_SIMULACRO',

  /** 422: la duracion maxima no es un entero positivo (INV-28). */
  DURACION_INVALIDA = 'DURACION_INVALIDA',

  /** 409: el estado actual no permite la operacion solicitada. */
  TRANSICION_NO_PERMITIDA = 'TRANSICION_NO_PERMITIDA',

  /** 409: el intento ya fue calificado una vez; la calificacion es inmutable (INV-32, CONTRATOS.md 8.3). */
  INTENTO_YA_CALIFICADO = 'INTENTO_YA_CALIFICADO',

  /** 500: una inconsistencia de datos que nunca deberia ocurrir en operacion normal. */
  ERROR_INTERNO = 'ERROR_INTERNO',
}
