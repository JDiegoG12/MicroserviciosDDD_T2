import { esLetraOpcion, LetraOpcion } from '../compartido/letra-opcion';
import { esNivelDificultad, NivelDificultad } from '../compartido/nivel-dificultad';
import { SolicitudInvalidaExcepcion } from '../excepciones/solicitud-invalida.excepcion';

/**
 * Una opcion de respuesta de una PreguntaEvaluable, tal como llega en el
 * evento `PreguntaPublicada` (CONTRATOS.md 7.4).
 */
export interface OpcionDePregunta {
  readonly letra: LetraOpcion;
  readonly texto: string;
}

/**
 * Terna de identificadores del Catalogo Academico que clasifica una
 * Pregunta. Solo identificadores, nunca copias de nombres (D-13,
 * MODELO-DOMINIO.md).
 */
export interface ClasificacionDePregunta {
  readonly competenciaId: string;
  readonly temaId: string;
  readonly subtemaId: string;
}

/**
 * Contenido completo de una PreguntaEvaluable, tal como lo entrega el
 * evento `PreguntaPublicada` (CONTRATOS.md 7.4). Es `null` en una
 * `PreguntaEvaluable` que solo existe como marca de archivo (su publicacion
 * aun no ha llegado, ver `PreguntaEvaluable.marcaDeArchivo`).
 */
export interface ContenidoDePregunta {
  readonly contexto: string;
  readonly preguntaDirecta: string;
  readonly opciones: readonly OpcionDePregunta[];
  readonly letraCorrecta: LetraOpcion;
  readonly clasificacion: ClasificacionDePregunta;
  readonly nivelDificultad: NivelDificultad;
  readonly fechaPublicacion: Date;
}

const LETRAS_ESPERADAS: readonly LetraOpcion[] = [
  LetraOpcion.A,
  LetraOpcion.B,
  LetraOpcion.C,
  LetraOpcion.D,
];

/**
 * Valida y construye un `ContenidoDePregunta` a partir de los datos crudos
 * del evento `PreguntaPublicada`.
 *
 * Verifica que las cuatro letras A-D esten presentes sin repetirse
 * (CONTRATOS.md 7.4: "Letras A-D en ese orden, sin repetir") y que
 * `letraCorrecta` sea una de las opciones. El orden de las opciones lo
 * define Editorial y no se reordena aqui.
 *
 * @param datos Contenido crudo, ya deserializado del JSON del evento.
 * @returns Un `ContenidoDePregunta` valido.
 * @throws SolicitudInvalidaExcepcion Si el contenido no cumple el contrato.
 */
export function crearContenidoDePregunta(datos: {
  contexto: string;
  preguntaDirecta: string;
  opciones: readonly OpcionDePregunta[];
  letraCorrecta: string;
  clasificacion: ClasificacionDePregunta;
  nivelDificultad: string;
  fechaPublicacion: Date;
}): ContenidoDePregunta {
  if (datos.contexto.trim().length === 0) {
    throw new SolicitudInvalidaExcepcion('El contexto de la pregunta no puede estar vacio.');
  }
  if (datos.preguntaDirecta.trim().length === 0) {
    throw new SolicitudInvalidaExcepcion('La pregunta directa no puede estar vacia.');
  }
  if (datos.opciones.length !== 4) {
    throw new SolicitudInvalidaExcepcion('La pregunta debe tener exactamente cuatro opciones.');
  }
  const letrasPresentes = new Set(datos.opciones.map((opcion) => opcion.letra));
  const tieneLasCuatroLetras = LETRAS_ESPERADAS.every((letra) => letrasPresentes.has(letra));
  if (!tieneLasCuatroLetras || letrasPresentes.size !== 4) {
    throw new SolicitudInvalidaExcepcion(
      'Las opciones de la pregunta deben tener las letras A, B, C y D sin repetir.',
    );
  }
  if (!esLetraOpcion(datos.letraCorrecta)) {
    throw new SolicitudInvalidaExcepcion('La letra correcta debe estar entre A y D.');
  }
  if (!letrasPresentes.has(datos.letraCorrecta)) {
    throw new SolicitudInvalidaExcepcion('La letra correcta debe ser una de las opciones de la pregunta.');
  }
  if (!esNivelDificultad(datos.nivelDificultad)) {
    throw new SolicitudInvalidaExcepcion('El nivel de dificultad debe ser BAJO, MEDIO o ALTO.');
  }

  return {
    contexto: datos.contexto,
    preguntaDirecta: datos.preguntaDirecta,
    opciones: datos.opciones,
    letraCorrecta: datos.letraCorrecta,
    clasificacion: datos.clasificacion,
    nivelDificultad: datos.nivelDificultad,
    fechaPublicacion: datos.fechaPublicacion,
  };
}
