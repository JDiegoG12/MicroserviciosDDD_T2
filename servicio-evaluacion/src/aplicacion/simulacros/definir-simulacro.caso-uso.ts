import { CriterioDeGeneracion } from '../../dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../dominio/simulacros/duracion-maxima';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { EnsambladorSimulacroServicio } from '../../dominio/servicios/ensamblador-simulacro.servicio';
import { Simulacro } from '../../dominio/simulacros/simulacro';
import { SimulacroRepositorio } from '../../dominio/simulacros/simulacro.repositorio';
import { Rol } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { PublicadorEventosPuerto } from '../puertos/salida/publicador-eventos.puerto';
import { RelojPuerto } from '../puertos/salida/reloj.puerto';
import { armarSimulacroRespuesta } from './armador-simulacro-respuesta';
import { DefinirSimulacroComando } from './dto/definir-simulacro.comando';
import { SimulacroRespuesta } from './dto/simulacro-respuesta';

/**
 * CU-13: Definir simulacro.
 *
 * Endpoint futuro: `POST /simulacros` (CONTRATOS.md 8.3), rol `DOCENTE`.
 */
export class DefinirSimulacroCasoUso implements CasoUso<DefinirSimulacroComando, SimulacroRespuesta> {
  constructor(
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly simulacroRepositorio: SimulacroRepositorio,
    private readonly ensamblador: EnsambladorSimulacroServicio,
    private readonly publicadorEventos: PublicadorEventosPuerto,
    private readonly reloj: RelojPuerto,
  ) {}

  public async ejecutar(comando: DefinirSimulacroComando): Promise<SimulacroRespuesta> {
    comando.usuario.exigirAlgunRol(Rol.DOCENTE);

    // Se construye la duracion antes de ensamblar para que DURACION_INVALIDA
    // no quede oculto detras de una busqueda de preguntas innecesaria.
    const duracion = DuracionMaxima.enMinutos(comando.duracionMaximaMinutos);
    const criterio = CriterioDeGeneracion.crear(comando.criterios);

    const candidatas = await this.preguntaEvaluableRepositorio.buscarPublicadasPorCriterios(criterio);
    const preguntasElegidas = this.ensamblador.ensamblar(candidatas, criterio, comando.cantidadPreguntas);

    const ahora = this.reloj.ahora();
    const simulacro = Simulacro.definir({
      docenteId: comando.usuario.id,
      nombre: comando.nombre,
      criterio,
      duracion,
      preguntas: preguntasElegidas,
      ahora,
    });

    await this.simulacroRepositorio.guardar(simulacro);
    await this.publicadorEventos.publicar(simulacro.extraerEventos());

    return armarSimulacroRespuesta(simulacro);
  }
}
