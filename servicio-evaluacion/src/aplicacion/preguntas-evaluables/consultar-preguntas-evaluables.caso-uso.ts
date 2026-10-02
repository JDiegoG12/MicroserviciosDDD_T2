import { NivelDificultad } from '../../dominio/compartido/nivel-dificultad';
import { EstadoPreguntaEvaluable } from '../../dominio/preguntas-evaluables/pregunta-evaluable';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { construirPagina, Pagina, validarPaginacion } from '../compartido/pagina';
import { aTextoIso } from '../compartido/formato-fecha';
import { Rol, UsuarioActual } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { PreguntaEvaluableRespuesta } from './dto/pregunta-evaluable-respuesta';

/**
 * Consulta de entrada de `ConsultarPreguntasEvaluablesCasoUso`.
 */
export interface ConsultarPreguntasEvaluablesConsulta {
  readonly usuario: UsuarioActual;
  readonly competenciaId?: string;
  readonly nivelDificultad?: NivelDificultad;
  readonly estado?: EstadoPreguntaEvaluable;
  readonly pagina?: number;
  readonly tamano?: number;
}

/**
 * `GET /preguntas-evaluables` (CONTRATOS.md 8.3), rol `DOCENTE`.
 *
 * Existe para demostrar en Postman que el evento `PreguntaPublicada` llego
 * a este servicio (CONTRATOS.md 8.3). Nunca expone `letraCorrecta`.
 */
export class ConsultarPreguntasEvaluablesCasoUso
  implements CasoUso<ConsultarPreguntasEvaluablesConsulta, Pagina<PreguntaEvaluableRespuesta>>
{
  constructor(private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio) {}

  public async ejecutar(
    consulta: ConsultarPreguntasEvaluablesConsulta,
  ): Promise<Pagina<PreguntaEvaluableRespuesta>> {
    consulta.usuario.exigirAlgunRol(Rol.DOCENTE);

    const { pagina, tamano } = validarPaginacion(consulta.pagina, consulta.tamano);

    const resultado = await this.preguntaEvaluableRepositorio.buscarPaginado(
      {
        competenciaId: consulta.competenciaId,
        nivelDificultad: consulta.nivelDificultad,
        estado: consulta.estado,
      },
      pagina,
      tamano,
    );

    // Una PreguntaEvaluable sin contenido es solo una marca de archivo que
    // aun no recibio su publicacion (orden no garantizado, CONTRATOS.md
    // 7.7.3): no hay nada que mostrar de ella todavia, asi que se omite de
    // esta vista.
    const contenido: PreguntaEvaluableRespuesta[] = resultado.elementos
      .filter((pregunta) => pregunta.contenido !== null)
      .map((pregunta) => {
        const vista = pregunta.vistaSinClave();
        return {
          preguntaId: vista.preguntaId,
          contexto: vista.contexto,
          preguntaDirecta: vista.preguntaDirecta,
          opciones: vista.opciones,
          clasificacion: vista.clasificacion,
          nivelDificultad: vista.nivelDificultad,
          estado: vista.estado,
          fechaPublicacion: aTextoIso(vista.fechaPublicacion),
          motivoArchivo: vista.motivoArchivo,
          fechaArchivado: vista.fechaArchivado ? aTextoIso(vista.fechaArchivado) : null,
          fechaActualizacion: aTextoIso(vista.fechaActualizacion),
        };
      });

    return construirPagina(contenido, pagina, tamano, resultado.totalElementos);
  }
}
