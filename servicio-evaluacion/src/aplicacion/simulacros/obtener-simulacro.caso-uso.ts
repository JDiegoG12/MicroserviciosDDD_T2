import { SimulacroId } from '../../dominio/simulacros/simulacro-id';
import { SimulacroRepositorio } from '../../dominio/simulacros/simulacro.repositorio';
import { SimulacroNoEncontradoExcepcion } from '../compartido/excepciones/simulacro-no-encontrado.excepcion';
import { Rol, UsuarioActual } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { armarSimulacroRespuesta } from './armador-simulacro-respuesta';
import { SimulacroRespuesta } from './dto/simulacro-respuesta';

/**
 * Consulta de entrada de `ObtenerSimulacroCasoUso`.
 */
export interface ObtenerSimulacroConsulta {
  readonly usuario: UsuarioActual;
  readonly simulacroId: string;
}

/**
 * Obtiene un Simulacro por su identificador.
 *
 * Endpoint futuro: `GET /simulacros/{simulacroId}` (CONTRATOS.md 8.3),
 * roles `DOCENTE` y `ESTUDIANTE`, sin claves (no expone `letraCorrecta`
 * porque `SimulacroRespuesta` solo lleva `preguntaId` y `posicion`).
 */
export class ObtenerSimulacroCasoUso implements CasoUso<ObtenerSimulacroConsulta, SimulacroRespuesta> {
  constructor(private readonly simulacroRepositorio: SimulacroRepositorio) {}

  public async ejecutar(consulta: ObtenerSimulacroConsulta): Promise<SimulacroRespuesta> {
    consulta.usuario.exigirAlgunRol(Rol.DOCENTE, Rol.ESTUDIANTE);

    const simulacroId = SimulacroId.desde(consulta.simulacroId);
    const simulacro = await this.simulacroRepositorio.obtenerPorId(simulacroId);
    if (!simulacro) {
      throw new SimulacroNoEncontradoExcepcion(consulta.simulacroId);
    }

    return armarSimulacroRespuesta(simulacro);
  }
}
