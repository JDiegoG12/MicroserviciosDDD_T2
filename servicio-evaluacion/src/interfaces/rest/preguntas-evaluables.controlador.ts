import { Controller, Get, Query } from '@nestjs/common';
import { ApiHeader, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { ConsultarPreguntasEvaluablesCasoUso } from '../../aplicacion/preguntas-evaluables/consultar-preguntas-evaluables.caso-uso';
import { UsuarioActual } from '../../aplicacion/compartido/usuario-actual';
import { Usuario } from './decoradores/usuario.decorator';
import { FiltrosPreguntasEvaluablesDto } from './dto/filtros-preguntas-evaluables.dto';

/**
 * `GET /preguntas-evaluables` (CONTRATOS.md 8.3): existe para demostrar en
 * Postman que el evento `PreguntaPublicada` llego a este servicio. Nunca
 * expone `letraCorrecta`.
 */
@ApiTags('Preguntas evaluables')
@ApiHeader({ name: 'X-Usuario-Id', required: true, description: 'UUID del usuario (CONTRATOS.md 4.1).' })
@ApiHeader({ name: 'X-Roles', required: true, description: 'Roles separados por comas (CONTRATOS.md 4.1).' })
@ApiHeader({ name: 'X-Id-Correlacion', required: false, description: 'Se genera si no llega (CONTRATOS.md 4.1).' })
@Controller('preguntas-evaluables')
export class PreguntasEvaluablesControlador {
  constructor(private readonly consultarPreguntasEvaluables: ConsultarPreguntasEvaluablesCasoUso) {}

  @Get()
  @ApiOperation({
    summary: 'Consulta la copia local de las preguntas publicadas o archivadas.',
    description: 'Rol requerido: DOCENTE. Nunca incluye letraCorrecta.',
  })
  @ApiResponse({ status: 200, description: 'Pagina de copias locales.' })
  public async consultar(@Usuario() usuario: UsuarioActual, @Query() filtros: FiltrosPreguntasEvaluablesDto) {
    return this.consultarPreguntasEvaluables.ejecutar({
      usuario,
      competenciaId: filtros.competenciaId,
      nivelDificultad: filtros.nivelDificultad,
      estado: filtros.estado,
      pagina: filtros.pagina,
      tamano: filtros.tamano,
    });
  }
}
