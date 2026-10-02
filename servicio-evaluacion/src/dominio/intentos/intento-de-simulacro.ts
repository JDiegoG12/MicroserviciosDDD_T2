import { RaizDeAgregado } from '../compartido/raiz-de-agregado';
import { esLetraOpcion, LetraOpcion } from '../compartido/letra-opcion';
import { esUuidValido } from '../compartido/uuid';
import { AccesoDenegadoExcepcion } from '../excepciones/acceso-denegado.excepcion';
import { InconsistenciaDeDatosExcepcion } from '../excepciones/inconsistencia-de-datos.excepcion';
import { IntentoFinalizadoExcepcion } from '../excepciones/intento-finalizado.excepcion';
import { IntentoYaCalificadoExcepcion } from '../excepciones/intento-ya-calificado.excepcion';
import { PreguntaNoPerteneceAlSimulacroExcepcion } from '../excepciones/pregunta-no-pertenece-al-simulacro.excepcion';
import { SolicitudInvalidaExcepcion } from '../excepciones/solicitud-invalida.excepcion';
import { TransicionNoPermitidaExcepcion } from '../excepciones/transicion-no-permitida.excepcion';
import { PreguntaSeleccionada } from '../simulacros/pregunta-seleccionada';
import { PreguntasSeleccionadas } from '../simulacros/preguntas-seleccionadas';
import { Simulacro } from '../simulacros/simulacro';
import { SimulacroId } from '../simulacros/simulacro-id';
import { Calificacion } from './calificacion';
import { EstadoIntento } from './estado-intento';
import { IntentoDeSimulacroCalificadoEvento } from './eventos/intento-de-simulacro-calificado.evento';
import {
  FinalizadorIntento,
  IntentoDeSimulacroFinalizadoEvento,
} from './eventos/intento-de-simulacro-finalizado.evento';
import { IntentoDeSimulacroIniciadoEvento } from './eventos/intento-de-simulacro-iniciado.evento';
import { IntentoId } from './intento-id';
import { RespuestaDelEstudiante } from './respuesta-del-estudiante';

interface PropiedadesIntentoDeSimulacro {
  readonly intentoId: IntentoId;
  readonly simulacroId: SimulacroId;
  readonly estudianteId: string;
  readonly preguntasDelSimulacro: PreguntasSeleccionadas;
  readonly fechaInicio: Date;
  readonly fechaLimite: Date;
  readonly estado: EstadoIntento;
  readonly fechaFinalizacion: Date | null;
  readonly finalizadoPor: FinalizadorIntento | null;
  readonly respuestas: ReadonlyMap<string, RespuestaDelEstudiante>;
  readonly calificacion: Calificacion | null;
}

/**
 * Agregado raiz IntentoDeSimulacro: la presentacion concreta de un
 * Simulacro por parte de un Estudiante (MODELO-DOMINIO.md B.3 §7.4), con su
 * instante de inicio, sus Respuestas del estudiante y, al cerrarse, su
 * Calificacion.
 *
 * Es un agregado autonomo respecto a Simulacro (B.3 §7.4: "separacion
 * responde a criterios de escalabilidad y concurrencia"): guarda su propia
 * copia de las preguntas seleccionadas para no tener que cargar el
 * Simulacro completo en cada operacion.
 *
 * INV-29 (MODELO-DOMINIO.md A.2): `simulacroId` y `estudianteId` son
 * obligatorios e inmutables desde que el intento se crea; ningun metodo los
 * cambia.
 */
export class IntentoDeSimulacro extends RaizDeAgregado {
  private estadoActual: EstadoIntento;
  private fechaFinalizacionActual: Date | null;
  private finalizadoPorActual: FinalizadorIntento | null;
  private calificacionActual: Calificacion | null;
  private readonly respuestas: Map<string, RespuestaDelEstudiante>;

  private constructor(private readonly propiedades: PropiedadesIntentoDeSimulacro) {
    super();
    this.estadoActual = propiedades.estado;
    this.fechaFinalizacionActual = propiedades.fechaFinalizacion;
    this.finalizadoPorActual = propiedades.finalizadoPor;
    this.calificacionActual = propiedades.calificacion;
    this.respuestas = new Map(propiedades.respuestas);
  }

  public get intentoId(): IntentoId {
    return this.propiedades.intentoId;
  }

  public get simulacroId(): SimulacroId {
    return this.propiedades.simulacroId;
  }

  public get estudianteId(): string {
    return this.propiedades.estudianteId;
  }

  public get fechaInicio(): Date {
    return this.propiedades.fechaInicio;
  }

  public get fechaLimite(): Date {
    return this.propiedades.fechaLimite;
  }

  public get estado(): EstadoIntento {
    return this.estadoActual;
  }

  public get fechaFinalizacion(): Date | null {
    return this.fechaFinalizacionActual;
  }

  public get finalizadoPor(): FinalizadorIntento | null {
    return this.finalizadoPorActual;
  }

  public get calificacion(): Calificacion | null {
    return this.calificacionActual;
  }

  /**
   * Abre un intento nuevo sobre un Simulacro (CU-14).
   *
   * Calcula `fechaLimite = fechaInicio + duracionMaximaMinutos`
   * (CONTRATOS.md 11.3) y guarda su propia copia de las preguntas
   * seleccionadas del simulacro, para validar la pertenencia de una
   * respuesta sin volver a cargar el Simulacro completo.
   *
   * @param simulacro El Simulacro que se va a presentar.
   * @param estudianteId Id del estudiante que presenta el intento (INV-29).
   * @param ahora Instante de inicio (RelojPuerto).
   * @returns El IntentoDeSimulacro abierto, con el evento
   * `IntentoDeSimulacroIniciado` ya registrado.
   * @throws SolicitudInvalidaExcepcion Si `estudianteId` no es un UUID
   * valido (INV-29, primera consecuencia).
   */
  public static iniciar(simulacro: Simulacro, estudianteId: string, ahora: Date): IntentoDeSimulacro {
    if (!esUuidValido(estudianteId)) {
      throw new SolicitudInvalidaExcepcion(
        'Todo intento pertenece a un unico estudiante identificado por un UUID valido (INV-29).',
      );
    }
    const fechaLimite = simulacro.duracion.sumarA(ahora);
    const intento = new IntentoDeSimulacro({
      intentoId: IntentoId.generar(),
      simulacroId: simulacro.simulacroId,
      estudianteId,
      preguntasDelSimulacro: simulacro.preguntas,
      fechaInicio: ahora,
      fechaLimite,
      estado: EstadoIntento.EN_CURSO,
      fechaFinalizacion: null,
      finalizadoPor: null,
      respuestas: new Map(),
      calificacion: null,
    });
    intento.registrarEvento(
      new IntentoDeSimulacroIniciadoEvento(
        ahora,
        intento.intentoId.aTexto(),
        intento.simulacroId.aTexto(),
        estudianteId,
      ),
    );
    return intento;
  }

  /**
   * Reconstruye un IntentoDeSimulacro desde su forma persistida. Uso
   * exclusivo de los repositorios (etapa 2).
   *
   * @param props Propiedades leidas de la persistencia.
   * @returns El IntentoDeSimulacro reconstruido, sin eventos pendientes.
   */
  public static reconstruir(props: PropiedadesIntentoDeSimulacro): IntentoDeSimulacro {
    return new IntentoDeSimulacro({ ...props });
  }

  /**
   * Comprueba que `estudianteId` sea el dueno de este intento (INV-29,
   * segunda consecuencia).
   *
   * @param estudianteId Id del usuario que intenta operar sobre el intento.
   * @throws AccesoDenegadoExcepcion Si no es el dueno.
   */
  public verificarPropietario(estudianteId: string): void {
    if (estudianteId !== this.propiedades.estudianteId) {
      throw new AccesoDenegadoExcepcion(
        'Solo el estudiante dueno puede operar sobre su propio intento (INV-29).',
      );
    }
  }

  /**
   * Vencimiento perezoso (CONTRATOS.md 11.3): si el intento esta EN_CURSO y
   * `ahora` ya alcanzo o supero `fechaLimite`, lo cierra aqui mismo con
   * `finalizadoPor = TIEMPO_AGOTADO` y registra
   * `IntentoDeSimulacroFinalizado`, **antes** de cualquier otra operacion.
   *
   * @param ahora Instante actual (RelojPuerto).
   * @returns `true` si el intento se acaba de cerrar por esta llamada.
   */
  public aplicarVencimientoPerezoso(ahora: Date): boolean {
    if (this.estadoActual === EstadoIntento.EN_CURSO && ahora.getTime() >= this.propiedades.fechaLimite.getTime()) {
      this.cerrarPorFinalizacion('TIEMPO_AGOTADO', this.propiedades.fechaLimite);
      return true;
    }
    return false;
  }

  private cerrarPorFinalizacion(por: FinalizadorIntento, fechaFinalizacion: Date): void {
    this.estadoActual = EstadoIntento.FINALIZADO;
    this.finalizadoPorActual = por;
    this.fechaFinalizacionActual = fechaFinalizacion;
    this.registrarEvento(
      new IntentoDeSimulacroFinalizadoEvento(fechaFinalizacion, this.propiedades.intentoId.aTexto(), por),
    );
  }

  /**
   * Registra la respuesta del estudiante a una pregunta del simulacro.
   *
   * Aplica primero el vencimiento perezoso. Como mucho una respuesta por
   * pregunta: la ultima reemplaza a la anterior (INV-30).
   *
   * @param preguntaId Id de la pregunta respondida.
   * @param letra Letra seleccionada, debe estar entre A y D.
   * @param ahora Instante de la operacion (RelojPuerto).
   * @throws IntentoFinalizadoExcepcion Si el intento ya no esta EN_CURSO,
   * sea porque el estudiante ya lo finalizo o porque acaba de vencer
   * (INV-31).
   * @throws SolicitudInvalidaExcepcion Si `letra` no esta entre A y D.
   * @throws PreguntaNoPerteneceAlSimulacroExcepcion Si `preguntaId` no es
   * una de las preguntas del simulacro de este intento (INV-29, tercera
   * consecuencia).
   */
  public registrarRespuesta(preguntaId: string, letra: string, ahora: Date): void {
    this.aplicarVencimientoPerezoso(ahora);
    if (this.estadoActual !== EstadoIntento.EN_CURSO) {
      throw new IntentoFinalizadoExcepcion(
        'El intento ya no esta en curso: no se pueden registrar mas respuestas (INV-31).',
      );
    }
    if (!esLetraOpcion(letra)) {
      throw new SolicitudInvalidaExcepcion('La letra seleccionada debe estar entre A y D.');
    }
    if (!this.propiedades.preguntasDelSimulacro.contiene(preguntaId)) {
      throw new PreguntaNoPerteneceAlSimulacroExcepcion(
        'La pregunta respondida no pertenece al simulacro de este intento (INV-29).',
      );
    }
    const respuesta: RespuestaDelEstudiante = { preguntaId, letraSeleccionada: letra as LetraOpcion };
    this.respuestas.set(preguntaId, respuesta);
  }

  /**
   * Finaliza el intento por decision del estudiante o por el sistema
   * (TIEMPO_AGOTADO). Aplica primero el vencimiento perezoso, que puede
   * terminar el intento por su cuenta.
   *
   * @param por Quien finaliza el intento.
   * @param ahora Instante de la operacion (RelojPuerto).
   * @throws IntentoFinalizadoExcepcion Si el intento ya no esta EN_CURSO
   * (ya estaba finalizado, calificado, o acaba de vencer por su cuenta).
   */
  public finalizar(por: FinalizadorIntento, ahora: Date): void {
    this.aplicarVencimientoPerezoso(ahora);
    if (this.estadoActual !== EstadoIntento.EN_CURSO) {
      throw new IntentoFinalizadoExcepcion('El intento ya esta finalizado.');
    }
    this.cerrarPorFinalizacion(por, ahora);
  }

  /**
   * Cierra el intento con su calificacion ya calculada por
   * `CalificadorSimulacroServicio`, y registra `IntentoDeSimulacroCalificado`
   * con todos los datos de CONTRATOS.md 7.6.
   *
   * Solo puede llamarse una vez, y solo sobre un intento FINALIZADO
   * (INV-32): la calificacion es inmutable.
   *
   * @param calificacion La calificacion ya calculada.
   * @throws IntentoYaCalificadoExcepcion Si el intento ya estaba CALIFICADO
   * (INV-32, CONTRATOS.md 8.3).
   * @throws TransicionNoPermitidaExcepcion Si el intento todavia esta
   * EN_CURSO (no se puede calificar sin finalizar antes) (INV-32).
   * @throws InconsistenciaDeDatosExcepcion Si `calificacion.totalPreguntas`
   * no coincide con la cantidad de preguntas del simulacro de este intento.
   */
  public cerrarConCalificacion(calificacion: Calificacion): void {
    if (this.estadoActual === EstadoIntento.CALIFICADO) {
      throw new IntentoYaCalificadoExcepcion('Un intento solo se puede calificar una vez (INV-32).');
    }
    if (this.estadoActual !== EstadoIntento.FINALIZADO) {
      throw new TransicionNoPermitidaExcepcion(
        'Un intento solo se puede calificar despues de finalizarlo (INV-32).',
      );
    }
    if (calificacion.totalPreguntas !== this.propiedades.preguntasDelSimulacro.ids().length) {
      throw new InconsistenciaDeDatosExcepcion(
        'El total de preguntas de la calificacion no coincide con las preguntas del simulacro.',
      );
    }
    // this.fechaFinalizacionActual ya esta fijada: el intento esta FINALIZADO.
    const fechaFinalizacion = this.fechaFinalizacionActual as Date;
    const finalizadoPor = this.finalizadoPorActual as FinalizadorIntento;

    this.estadoActual = EstadoIntento.CALIFICADO;
    this.calificacionActual = calificacion;
    this.registrarEvento(
      new IntentoDeSimulacroCalificadoEvento(
        fechaFinalizacion,
        this.propiedades.intentoId.aTexto(),
        this.propiedades.simulacroId.aTexto(),
        this.propiedades.estudianteId,
        this.propiedades.fechaInicio,
        fechaFinalizacion,
        finalizadoPor,
        calificacion,
      ),
    );
  }

  /**
   * Respuesta registrada para una pregunta, si existe.
   *
   * @param preguntaId Id de la pregunta.
   * @returns La respuesta del estudiante, o `undefined` si no respondio.
   */
  public respuestaPara(preguntaId: string): RespuestaDelEstudiante | undefined {
    return this.respuestas.get(preguntaId);
  }

  /**
   * Ids de todas las preguntas del simulacro de este intento, en orden de
   * posicion.
   *
   * @returns La lista de preguntaId.
   */
  public preguntaIds(): string[] {
    return this.propiedades.preguntasDelSimulacro.ids();
  }

  /**
   * Preguntas seleccionadas del simulacro de este intento, con su
   * `posicion`, en el mismo orden con el que se definio el simulacro. Uso
   * principal: armar la vista de un IntentoRespuesta con la posicion
   * correcta de cada pregunta (CONTRATOS.md 8.3).
   *
   * @returns Una copia de la lista de PreguntaSeleccionada.
   */
  public preguntasSeleccionadas(): PreguntaSeleccionada[] {
    return this.propiedades.preguntasDelSimulacro.comoLista();
  }

  /**
   * Todas las respuestas registradas hasta ahora.
   *
   * @returns Una copia de las respuestas, sin orden garantizado.
   */
  public todasLasRespuestas(): RespuestaDelEstudiante[] {
    return [...this.respuestas.values()];
  }
}
