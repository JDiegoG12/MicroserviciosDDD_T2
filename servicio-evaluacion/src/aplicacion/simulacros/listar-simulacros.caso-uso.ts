import { SimulacroRepositorio } from '../../dominio/simulacros/simulacro.repositorio';
import { construirPagina, Pagina, validarPaginacion } from '../compartido/pagina';
import { Rol, UsuarioActual } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { armarSimulacroResumen } from './armador-simulacro-respuesta';
import { SimulacroResumen } from './dto/simulacro-respuesta';

/**
 * Consulta de entrada de `ListarSimulacrosCasoUso`.
 */
export interface ListarSimulacrosConsulta {
  readonly usuario: UsuarioActual;
  readonly pagina?: number;
  readonly tamano?: number;
}

/**
 * Lista todos los Simulacros definidos, paginados (CONTRATOS.md 5.1 y 8.3).
 *
 * Endpoint: `GET /simulacros` (CONTRATOS.md 8.3), roles `DOCENTE` y
 * `ESTUDIANTE`. Todos los simulacros estan disponibles para todos los
 * estudiantes (MODELO-DOMINIO.md A.3: "No hay grupos ni asignaciones"), asi
 * que no hay restriccion adicional por usuario.
 *
 * La paginacion se aplica en memoria sobre el resultado de
 * `SimulacroRepositorio.listar()`: el puerto de dominio no cambia (sigue
 * devolviendo todos los simulacros), porque el volumen esperado de
 * simulacros de este taller es pequeno.
 */
export class ListarSimulacrosCasoUso implements CasoUso<ListarSimulacrosConsulta, Pagina<SimulacroResumen>> {
  constructor(private readonly simulacroRepositorio: SimulacroRepositorio) {}

  public async ejecutar(consulta: ListarSimulacrosConsulta): Promise<Pagina<SimulacroResumen>> {
    consulta.usuario.exigirAlgunRol(Rol.DOCENTE, Rol.ESTUDIANTE);

    const { pagina, tamano } = validarPaginacion(consulta.pagina, consulta.tamano);

    const simulacros = await this.simulacroRepositorio.listar();
    const inicio = pagina * tamano;
    const contenido = simulacros.slice(inicio, inicio + tamano).map(armarSimulacroResumen);

    return construirPagina(contenido, pagina, tamano, simulacros.length);
  }
}
