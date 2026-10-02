import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsOptional, IsUUID } from 'class-validator';
import { NivelDificultad } from '../../../dominio/compartido/nivel-dificultad';
import { EstadoPreguntaEvaluable } from '../../../dominio/preguntas-evaluables/pregunta-evaluable';
import { NormalizarUuid } from './normalizar-uuid.transformador';
import { PaginacionDto } from './paginacion.dto';

/**
 * Filtros de `GET /preguntas-evaluables` (CONTRATOS.md 8.3).
 */
export class FiltrosPreguntasEvaluablesDto extends PaginacionDto {
  @ApiPropertyOptional({ format: 'uuid' })
  @IsOptional()
  @NormalizarUuid()
  @IsUUID('4')
  competenciaId?: string;

  @ApiPropertyOptional({ enum: NivelDificultad })
  @IsOptional()
  @IsEnum(NivelDificultad)
  nivelDificultad?: NivelDificultad;

  @ApiPropertyOptional({ enum: EstadoPreguntaEvaluable })
  @IsOptional()
  @IsEnum(EstadoPreguntaEvaluable)
  estado?: EstadoPreguntaEvaluable;
}
