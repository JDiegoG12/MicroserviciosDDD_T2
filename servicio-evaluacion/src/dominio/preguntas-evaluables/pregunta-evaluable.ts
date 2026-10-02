import { esUuidValido } from '../compartido/uuid';
import { SolicitudInvalidaExcepcion } from '../excepciones/solicitud-invalida.excepcion';
import { ContenidoDePregunta } from './contenido-de-pregunta';
import { VistaPreguntaEvaluable } from './vista-pregunta-evaluable';

/**
 * Estado de una PreguntaEvaluable dentro de este servicio (CONTRATOS.md
 * 11.3). No tiene relacion directa con los ocho estados del ciclo de vida
 * de la Pregunta en Editorial: solo distingue si la copia local puede
 * usarse para ensamblar simulacros nuevos.
 */
export enum EstadoPreguntaEvaluable {
  PUBLICADA = 'PUBLICADA',
  ARCHIVADA = 'ARCHIVADA',
}

interface PropiedadesPreguntaEvaluable {
  readonly preguntaId: string;
  readonly contenido: ContenidoDePregunta | null;
  readonly estado: EstadoPreguntaEvaluable;
  readonly motivoArchivo: string | null;
  readonly fechaArchivado: Date | null;
  readonly fechaActualizacion: Date;
}

/**
 * Copia local e inmutable de una Pregunta publicada por Gestion Editorial de
 * Preguntas, el "item cerrado" del Taller 1 (CONTRATOS.md 11.3:
 * "No es un agregado editable: solo lo crean o archivan los consumidores").
 *
 * Se alimenta exclusivamente de los eventos `PreguntaPublicada` y
 * `PreguntaArchivada` (CONTRATOS.md 7.4, 7.5 y 7.7.3). Este servicio nunca
 * modifica su contenido: una vez que tiene `contenido`, ese contenido es
 * definitivo (MODELO-DOMINIO.md A.2, INV-32: "la copia local de la pregunta
 * nunca se modifica, solo cambia su estado").
 */
export class PreguntaEvaluable {
  private constructor(private readonly propiedades: PropiedadesPreguntaEvaluable) {}

  public get preguntaId(): string {
    return this.propiedades.preguntaId;
  }

  public get contenido(): ContenidoDePregunta | null {
    return this.propiedades.contenido;
  }

  public get estado(): EstadoPreguntaEvaluable {
    return this.propiedades.estado;
  }

  /** Motivo del archivado (CONTRATOS.md 7.5 y 8.3), o `null` si nunca se archivo. */
  public get motivoArchivo(): string | null {
    return this.propiedades.motivoArchivo;
  }

  /** Fecha en que se archivo (CONTRATOS.md 7.5 y 8.3), o `null` si nunca se archivo. */
  public get fechaArchivado(): Date | null {
    return this.propiedades.fechaArchivado;
  }

  public get fechaActualizacion(): Date {
    return this.propiedades.fechaActualizacion;
  }

  /**
   * Crea la copia local a partir de un evento `PreguntaPublicada`
   * (CONTRATOS.md 7.4) para una pregunta que esta aun no conocia.
   *
   * @param preguntaId Id de la Pregunta en Editorial (sera el `_id` local).
   * @param contenido Contenido ya validado de la pregunta.
   * @param ahora Instante en el que se procesa el evento (RelojPuerto).
   * @returns Una PreguntaEvaluable nueva en estado PUBLICADA.
   * @throws SolicitudInvalidaExcepcion Si `preguntaId` no es un UUID valido.
   */
  public static desdePublicacion(
    preguntaId: string,
    contenido: ContenidoDePregunta,
    ahora: Date,
  ): PreguntaEvaluable {
    if (!esUuidValido(preguntaId)) {
      throw new SolicitudInvalidaExcepcion('El preguntaId debe ser un UUID valido.');
    }
    return new PreguntaEvaluable({
      preguntaId,
      contenido,
      estado: EstadoPreguntaEvaluable.PUBLICADA,
      motivoArchivo: null,
      fechaArchivado: null,
      fechaActualizacion: ahora,
    });
  }

  /**
   * Crea una marca de archivo para una pregunta que este servicio aun no
   * conocia: el evento `PreguntaArchivada` llego antes que su
   * `PreguntaPublicada` (orden no garantizado, CONTRATOS.md 7.7.3: "si
   * después llega su PreguntaPublicada, la pregunta queda archivada").
   *
   * @param preguntaId Id de la Pregunta en Editorial.
   * @param motivo Motivo del archivado (CONTRATOS.md 7.5), de 1 a 500 caracteres.
   * @param fechaArchivado Fecha en que se archivo (CONTRATOS.md 7.5).
   * @param ahora Instante en el que se procesa el evento.
   * @returns Una PreguntaEvaluable sin contenido, en estado ARCHIVADA.
   * @throws SolicitudInvalidaExcepcion Si `preguntaId` no es un UUID valido
   * o `motivo` esta vacio.
   */
  public static marcaDeArchivo(
    preguntaId: string,
    motivo: string,
    fechaArchivado: Date,
    ahora: Date,
  ): PreguntaEvaluable {
    if (!esUuidValido(preguntaId)) {
      throw new SolicitudInvalidaExcepcion('El preguntaId debe ser un UUID valido.');
    }
    if (motivo.trim().length === 0) {
      throw new SolicitudInvalidaExcepcion('El motivo del archivado no puede estar vacio.');
    }
    return new PreguntaEvaluable({
      preguntaId,
      contenido: null,
      estado: EstadoPreguntaEvaluable.ARCHIVADA,
      motivoArchivo: motivo,
      fechaArchivado,
      fechaActualizacion: ahora,
    });
  }

  /**
   * Reconstruye una PreguntaEvaluable desde sus propiedades persistidas.
   * Uso exclusivo de los repositorios (etapa 2); no valida de nuevo el
   * contenido porque ya se valido al guardarlo.
   *
   * @param props Propiedades leidas de la persistencia.
   * @returns La PreguntaEvaluable reconstruida.
   */
  public static reconstruir(props: PropiedadesPreguntaEvaluable): PreguntaEvaluable {
    return new PreguntaEvaluable({ ...props });
  }

  /**
   * Aplica un evento `PreguntaArchivada` sobre una copia que ya existe.
   *
   * Conserva el contenido existente (si lo tiene) para que los intentos ya
   * iniciados con esta pregunta se sigan calificando igual (CONTRATOS.md
   * 11.3: "Archivar una pregunta no altera simulacros ni intentos
   * existentes"). El contenido nunca se modifica (INV-32/A.2): esta
   * operacion solo cambia el estado, `motivoArchivo`, `fechaArchivado` y
   * `fechaActualizacion`.
   *
   * @param motivo Motivo del archivado (CONTRATOS.md 7.5), de 1 a 500 caracteres.
   * @param fechaArchivado Fecha en que se archivo (CONTRATOS.md 7.5).
   * @param ahora Instante en el que se procesa el evento.
   * @returns Una copia nueva en estado ARCHIVADA con el mismo contenido.
   * @throws SolicitudInvalidaExcepcion Si `motivo` esta vacio.
   */
  public archivar(motivo: string, fechaArchivado: Date, ahora: Date): PreguntaEvaluable {
    if (motivo.trim().length === 0) {
      throw new SolicitudInvalidaExcepcion('El motivo del archivado no puede estar vacio.');
    }
    return new PreguntaEvaluable({
      preguntaId: this.propiedades.preguntaId,
      contenido: this.propiedades.contenido,
      estado: EstadoPreguntaEvaluable.ARCHIVADA,
      motivoArchivo: motivo,
      fechaArchivado,
      fechaActualizacion: ahora,
    });
  }

  /**
   * Aplica un evento `PreguntaPublicada` sobre una copia que ya existe
   * (orden no garantizado: la marca de archivo llego primero, CONTRATOS.md
   * 7.7.3).
   *
   * Regla de orden (7.7.3): una pregunta archivada sigue archivada aunque
   * despues llegue su publicacion, asi que el estado, `motivoArchivo` y
   * `fechaArchivado` **nunca** cambian aqui. Solo completa el contenido si
   * la copia todavia no lo tenia; si ya tenia contenido, esta operacion no
   * hace nada (INV-32/A.2: la copia local nunca se modifica una vez tiene
   * contenido).
   *
   * @param contenido Contenido ya validado de la pregunta.
   * @param ahora Instante en el que se procesa el evento.
   * @returns Una copia con el contenido completo, en el mismo estado.
   */
  public incorporarPublicacion(contenido: ContenidoDePregunta, ahora: Date): PreguntaEvaluable {
    if (this.propiedades.contenido !== null) {
      return this;
    }
    return new PreguntaEvaluable({
      preguntaId: this.propiedades.preguntaId,
      contenido,
      estado: this.propiedades.estado,
      motivoArchivo: this.propiedades.motivoArchivo,
      fechaArchivado: this.propiedades.fechaArchivado,
      fechaActualizacion: ahora,
    });
  }

  /**
   * Indica si esta copia esta disponible para ensamblar simulacros nuevos
   * (INV-25).
   *
   * @returns `true` si el estado es PUBLICADA.
   */
  public estaPublicada(): boolean {
    return this.propiedades.estado === EstadoPreguntaEvaluable.PUBLICADA;
  }

  /**
   * Compara una letra de respuesta contra la letra correcta de esta copia.
   *
   * @param letra Letra seleccionada por el estudiante.
   * @returns `true` si coincide con `letraCorrecta`.
   * @throws InconsistenciaDeDatosExcepcion Si la copia no tiene contenido.
   */
  public esCorrecta(letra: string): boolean {
    if (this.propiedades.contenido === null) {
      throw new SolicitudInvalidaExcepcion(
        'No se puede calificar una pregunta sin contenido (solo se registro su archivo).',
      );
    }
    return this.propiedades.contenido.letraCorrecta === letra;
  }

  /**
   * Construye la vista para el estudiante o el docente, sin `letraCorrecta`
   * (CONTRATOS.md 7.4, 8.3 y 11.3).
   *
   * @returns La vista publica de esta pregunta.
   * @throws SolicitudInvalidaExcepcion Si la copia no tiene contenido.
   */
  public vistaSinClave(): VistaPreguntaEvaluable {
    if (this.propiedades.contenido === null) {
      throw new SolicitudInvalidaExcepcion(
        'No se puede mostrar una pregunta sin contenido (solo se registro su archivo).',
      );
    }
    return {
      preguntaId: this.propiedades.preguntaId,
      contexto: this.propiedades.contenido.contexto,
      preguntaDirecta: this.propiedades.contenido.preguntaDirecta,
      opciones: this.propiedades.contenido.opciones,
      clasificacion: this.propiedades.contenido.clasificacion,
      nivelDificultad: this.propiedades.contenido.nivelDificultad,
      estado: this.propiedades.estado,
      fechaPublicacion: this.propiedades.contenido.fechaPublicacion,
      motivoArchivo: this.propiedades.motivoArchivo,
      fechaArchivado: this.propiedades.fechaArchivado,
      fechaActualizacion: this.propiedades.fechaActualizacion,
    };
  }
}
