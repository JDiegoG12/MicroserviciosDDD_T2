import { RaizDeAgregado } from '../compartido/raiz-de-agregado';
import { esUuidValido } from '../compartido/uuid';
import { PreguntaEvaluable } from '../preguntas-evaluables/pregunta-evaluable';
import { PreguntaNoPublicadaExcepcion } from '../excepciones/pregunta-no-publicada.excepcion';
import { SolicitudInvalidaExcepcion } from '../excepciones/solicitud-invalida.excepcion';
import { CriterioDeGeneracion } from './criterio-de-generacion';
import { DuracionMaxima } from './duracion-maxima';
import { SimulacroDefinidoEvento } from './eventos/simulacro-definido.evento';
import { PreguntasSeleccionadas } from './preguntas-seleccionadas';
import { SimulacroId } from './simulacro-id';

interface PropiedadesSimulacro {
  readonly simulacroId: SimulacroId;
  readonly docenteId: string;
  readonly nombre: string;
  readonly criterio: CriterioDeGeneracion;
  readonly duracion: DuracionMaxima;
  readonly preguntas: PreguntasSeleccionadas;
  readonly fechaCreacion: Date;
}

/**
 * Agregado raiz Simulacro: la plantilla reutilizable de una evaluacion
 * (MODELO-DOMINIO.md B.3 §7.3), que un Docente define con unos criterios de
 * generacion, una duracion maxima y un conjunto de Preguntas Publicadas
 * (CU-13, CONTRATOS.md 8.3).
 *
 * No es la presentacion de la prueba (eso es IntentoDeSimulacro, un
 * agregado separado): es la definicion, de la que se originan muchos
 * intentos.
 */
export class Simulacro extends RaizDeAgregado {
  private constructor(private propiedades: PropiedadesSimulacro) {
    super();
  }

  public get simulacroId(): SimulacroId {
    return this.propiedades.simulacroId;
  }

  public get docenteId(): string {
    return this.propiedades.docenteId;
  }

  public get nombre(): string {
    return this.propiedades.nombre;
  }

  public get criterio(): CriterioDeGeneracion {
    return this.propiedades.criterio;
  }

  public get duracion(): DuracionMaxima {
    return this.propiedades.duracion;
  }

  public get preguntas(): PreguntasSeleccionadas {
    return this.propiedades.preguntas;
  }

  public get fechaCreacion(): Date {
    return this.propiedades.fechaCreacion;
  }

  /**
   * Define un Simulacro nuevo (CU-13).
   *
   * Valida, en orden: que el nombre no este vacio; INV-25 (RF-21), que
   * todas las preguntas ya seleccionadas por el ensamblador esten
   * PUBLICADA (una defensa adicional: `EnsambladorSimulacroServicio` ya
   * debio filtrarlas, pero el agregado no confia ciegamente en su
   * invocador); e INV-26 e INV-27 a traves de `PreguntasSeleccionadas`.
   *
   * @param datos Datos de la definicion, con las preguntas ya elegidas por
   * `EnsambladorSimulacroServicio`.
   * @returns El Simulacro definido, con el evento `SimulacroDefinido` ya
   * registrado internamente.
   * @throws SolicitudInvalidaExcepcion Si el docenteId no es un UUID valido
   * o el nombre esta vacio.
   * @throws PreguntaNoPublicadaExcepcion Si alguna pregunta no esta
   * PUBLICADA (INV-25, CONTRATOS.md 8.3).
   * @throws PreguntasInsuficientesExcepcion Si la lista esta vacia (INV-26).
   * @throws PreguntaDuplicadaEnSimulacroExcepcion Si hay preguntas repetidas (INV-27, CONTRATOS.md 8.3).
   */
  public static definir(datos: {
    docenteId: string;
    nombre: string;
    criterio: CriterioDeGeneracion;
    duracion: DuracionMaxima;
    preguntas: readonly PreguntaEvaluable[];
    ahora: Date;
  }): Simulacro {
    if (!esUuidValido(datos.docenteId)) {
      throw new SolicitudInvalidaExcepcion('El docenteId debe ser un UUID valido.');
    }
    if (datos.nombre.trim().length === 0) {
      throw new SolicitudInvalidaExcepcion('El nombre del simulacro no puede estar vacio.');
    }
    const todasPublicadas = datos.preguntas.every((pregunta) => pregunta.estaPublicada());
    if (!todasPublicadas) {
      throw new PreguntaNoPublicadaExcepcion(
        'Un simulacro solo puede componerse de preguntas publicadas (INV-25).',
      );
    }

    const preguntasSeleccionadas = PreguntasSeleccionadas.desdePreguntas(datos.preguntas);

    const simulacro = new Simulacro({
      simulacroId: SimulacroId.generar(),
      docenteId: datos.docenteId,
      nombre: datos.nombre,
      criterio: datos.criterio,
      duracion: datos.duracion,
      preguntas: preguntasSeleccionadas,
      fechaCreacion: datos.ahora,
    });
    simulacro.registrarEvento(
      new SimulacroDefinidoEvento(datos.ahora, simulacro.simulacroId.aTexto(), simulacro.docenteId),
    );
    return simulacro;
  }

  /**
   * Reconstruye un Simulacro desde su forma persistida. Uso exclusivo de
   * los repositorios (etapa 2).
   *
   * @param props Propiedades leidas de la persistencia.
   * @returns El Simulacro reconstruido, sin eventos pendientes.
   */
  public static reconstruir(props: PropiedadesSimulacro): Simulacro {
    return new Simulacro({ ...props });
  }
}
