import { RegistrarPreguntaArchivadaComando } from '../../aplicacion/preguntas-evaluables/dto/registrar-pregunta-archivada.comando';
import { RegistrarPreguntaPublicadaComando } from '../../aplicacion/preguntas-evaluables/dto/registrar-pregunta-publicada.comando';
import { MensajeInvalidoExcepcion } from './mensaje-invalido.excepcion';

/** Sobre ya deserializado, antes de validar su contenido. */
export interface SobreCrudo {
  readonly idEvento?: unknown;
  readonly tipoEvento?: unknown;
  readonly versionEvento?: unknown;
  readonly idCorrelacion?: unknown;
  readonly datos?: unknown;
}

/**
 * Interpreta el contenido crudo de un mensaje de la cola
 * `evaluacion.preguntas` y comprueba las reglas comunes del sobre
 * (CONTRATOS.md 7.7.4): JSON legible, `versionEvento === 1` y
 * `tipoEvento` conocido.
 *
 * Lector tolerante: no usa los esquemas estrictos de `/contratos` (esos
 * describen lo que el productor debe enviar); valida solo lo que este
 * consumidor necesita, y un campo adicional desconocido nunca lo invalida.
 *
 * @param contenido Bytes crudos del cuerpo del mensaje AMQP.
 * @returns El sobre ya tipado, con `tipoEvento` acotado a los dos valores
 * que este servicio consume.
 * @throws MensajeInvalidoExcepcion Si el JSON no se puede leer, falta
 * `idEvento`, `versionEvento` no es 1, o `tipoEvento` no es
 * `PreguntaPublicada` ni `PreguntaArchivada`.
 */
export function interpretarSobre(contenido: Buffer): {
  idEvento: string;
  tipoEvento: 'PreguntaPublicada' | 'PreguntaArchivada';
  idCorrelacion: string | null;
  datos: Record<string, unknown>;
} {
  let sobre: SobreCrudo;
  try {
    sobre = JSON.parse(contenido.toString('utf-8')) as SobreCrudo;
  } catch {
    throw new MensajeInvalidoExcepcion('El cuerpo del mensaje no es un JSON valido.');
  }

  if (typeof sobre.idEvento !== 'string' || sobre.idEvento.length === 0) {
    throw new MensajeInvalidoExcepcion('Falta el campo obligatorio idEvento.');
  }
  if (sobre.versionEvento !== 1) {
    throw new MensajeInvalidoExcepcion(`versionEvento debe ser 1, llego ${String(sobre.versionEvento)}.`);
  }
  if (sobre.tipoEvento !== 'PreguntaPublicada' && sobre.tipoEvento !== 'PreguntaArchivada') {
    throw new MensajeInvalidoExcepcion(`tipoEvento desconocido: ${String(sobre.tipoEvento)}.`);
  }
  if (typeof sobre.datos !== 'object' || sobre.datos === null) {
    throw new MensajeInvalidoExcepcion('Falta el campo obligatorio datos.');
  }

  return {
    idEvento: sobre.idEvento,
    tipoEvento: sobre.tipoEvento,
    idCorrelacion: typeof sobre.idCorrelacion === 'string' ? sobre.idCorrelacion : null,
    datos: sobre.datos as Record<string, unknown>,
  };
}

function exigirTexto(datos: Record<string, unknown>, campo: string): string {
  const valor = datos[campo];
  if (typeof valor !== 'string' || valor.length === 0) {
    throw new MensajeInvalidoExcepcion(`El campo datos.${campo} es obligatorio y debe ser un texto.`);
  }
  return valor;
}

function exigirFecha(datos: Record<string, unknown>, campo: string): Date {
  const valor = exigirTexto(datos, campo);
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    throw new MensajeInvalidoExcepcion(`El campo datos.${campo} debe ser una fecha ISO-8601 valida.`);
  }
  return fecha;
}

/**
 * Valida los campos obligatorios de `PreguntaPublicada` que este servicio
 * usa (CONTRATOS.md 7.4) y arma el comando de
 * `RegistrarPreguntaPublicadaCasoUso`.
 *
 * @param idEvento Id del evento (ya validado por `interpretarSobre`).
 * @param datos Campo `datos` del sobre, ya comprobado como objeto.
 * @returns El comando listo para el caso de uso.
 * @throws MensajeInvalidoExcepcion Si falta o tiene tipo incorrecto algun
 * campo obligatorio que el consumidor usa.
 */
export function validarMensajePreguntaPublicada(
  idEvento: string,
  datos: Record<string, unknown>,
): RegistrarPreguntaPublicadaComando {
  const preguntaId = exigirTexto(datos, 'preguntaId');
  const contexto = exigirTexto(datos, 'contexto');
  const preguntaDirecta = exigirTexto(datos, 'preguntaDirecta');
  const letraCorrecta = exigirTexto(datos, 'letraCorrecta');
  const nivelDificultad = exigirTexto(datos, 'nivelDificultad');
  const fechaPublicacion = exigirFecha(datos, 'fechaPublicacion');

  const opcionesCrudas = datos.opciones;
  if (!Array.isArray(opcionesCrudas) || opcionesCrudas.length !== 4) {
    throw new MensajeInvalidoExcepcion('El campo datos.opciones debe ser un arreglo de cuatro elementos.');
  }
  const opciones = opcionesCrudas.map((opcion: unknown) => {
    if (
      typeof opcion !== 'object' ||
      opcion === null ||
      typeof (opcion as { letra?: unknown }).letra !== 'string' ||
      typeof (opcion as { texto?: unknown }).texto !== 'string'
    ) {
      throw new MensajeInvalidoExcepcion('Cada elemento de datos.opciones debe tener letra y texto de texto.');
    }
    return { letra: (opcion as { letra: string }).letra, texto: (opcion as { texto: string }).texto };
  });

  const clasificacionCruda = datos.clasificacion;
  if (typeof clasificacionCruda !== 'object' || clasificacionCruda === null) {
    throw new MensajeInvalidoExcepcion('El campo datos.clasificacion es obligatorio.');
  }
  const clasificacion = clasificacionCruda as Record<string, unknown>;
  const competenciaId = exigirTexto(clasificacion, 'competenciaId');
  const temaId = exigirTexto(clasificacion, 'temaId');
  const subtemaId = exigirTexto(clasificacion, 'subtemaId');

  return {
    idEvento,
    preguntaId,
    contexto,
    preguntaDirecta,
    opciones: opciones as RegistrarPreguntaPublicadaComando['opciones'],
    letraCorrecta,
    clasificacion: { competenciaId, temaId, subtemaId },
    nivelDificultad,
    fechaPublicacion,
  };
}

/**
 * Valida los campos obligatorios de `PreguntaArchivada` (CONTRATOS.md 7.5)
 * y arma el comando de `RegistrarPreguntaArchivadaCasoUso`.
 *
 * @param idEvento Id del evento (ya validado por `interpretarSobre`).
 * @param datos Campo `datos` del sobre, ya comprobado como objeto.
 * @returns El comando listo para el caso de uso.
 * @throws MensajeInvalidoExcepcion Si falta o tiene tipo incorrecto algun
 * campo obligatorio.
 */
export function validarMensajePreguntaArchivada(
  idEvento: string,
  datos: Record<string, unknown>,
): RegistrarPreguntaArchivadaComando {
  return {
    idEvento,
    preguntaId: exigirTexto(datos, 'preguntaId'),
    motivo: exigirTexto(datos, 'motivo'),
    fechaArchivado: exigirFecha(datos, 'fechaArchivado'),
  };
}
