import { IntentoDeSimulacro } from '../../dominio/intentos/intento-de-simulacro';
import { IntentoDeSimulacroRepositorio } from '../../dominio/intentos/intento-de-simulacro.repositorio';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { SimulacroId } from '../../dominio/simulacros/simulacro-id';
import { SimulacroRepositorio } from '../../dominio/simulacros/simulacro.repositorio';
import { SimulacroNoEncontradoExcepcion } from '../compartido/excepciones/simulacro-no-encontrado.excepcion';
import { Rol } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { PublicadorEventosPuerto } from '../puertos/salida/publicador-eventos.puerto';
import { RelojPuerto } from '../puertos/salida/reloj.puerto';
import { armarIntentoRespuesta } from './armador-intento-respuesta';
import { IniciarIntentoComando } from './dto/iniciar-intento.comando';
import { IntentoRespuesta } from './dto/intento-respuesta';

/**
 * CU-14 (apertura): Presentar simulacro, primer paso.
 *
 * Endpoint futuro: `POST /simulacros/{simulacroId}/intentos`
 * (CONTRATOS.md 8.3), rol `ESTUDIANTE`.
 *
 * Todos los simulacros definidos estan disponibles para todos los
 * estudiantes (MODELO-DOMINIO.md A.3), asi que no hay una verificacion de
 * disponibilidad adicional mas alla de que el simulacro exista.
 */
export class IniciarIntentoCasoUso implements CasoUso<IniciarIntentoComando, IntentoRespuesta> {
  constructor(
    private readonly simulacroRepositorio: SimulacroRepositorio,
    private readonly intentoDeSimulacroRepositorio: IntentoDeSimulacroRepositorio,
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly publicadorEventos: PublicadorEventosPuerto,
    private readonly reloj: RelojPuerto,
  ) {}

  public async ejecutar(comando: IniciarIntentoComando): Promise<IntentoRespuesta> {
    comando.usuario.exigirAlgunRol(Rol.ESTUDIANTE);

    const simulacroId = SimulacroId.desde(comando.simulacroId);
    const simulacro = await this.simulacroRepositorio.obtenerPorId(simulacroId);
    if (!simulacro) {
      throw new SimulacroNoEncontradoExcepcion(comando.simulacroId);
    }

    const ahora = this.reloj.ahora();
    const intento = IntentoDeSimulacro.iniciar(simulacro, comando.usuario.id, ahora);

    await this.intentoDeSimulacroRepositorio.guardar(intento);
    await this.publicadorEventos.publicar(intento.extraerEventos());

    const preguntas = await this.preguntaEvaluableRepositorio.obtenerPorIds(intento.preguntaIds());
    return armarIntentoRespuesta(intento, preguntas);
  }
}
