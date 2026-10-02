/**
 * Tokens de inyeccion de dependencias para los puertos del dominio y de la
 * aplicacion (interfaces de TypeScript, que no existen en tiempo de
 * ejecucion y por eso NestJS necesita un token explicito para resolverlas).
 */
export const PREGUNTA_EVALUABLE_REPOSITORIO = Symbol('PreguntaEvaluableRepositorio');
export const SIMULACRO_REPOSITORIO = Symbol('SimulacroRepositorio');
export const INTENTO_DE_SIMULACRO_REPOSITORIO = Symbol('IntentoDeSimulacroRepositorio');
export const REGISTRO_EVENTOS_PROCESADOS_PUERTO = Symbol('RegistroEventosProcesadosPuerto');
export const PUBLICADOR_EVENTOS_PUERTO = Symbol('PublicadorEventosPuerto');
export const RELOJ_PUERTO = Symbol('RelojPuerto');
export const FUENTE_ALEATORIA = Symbol('FuenteAleatoria');
