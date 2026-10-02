import { ApiProperty } from '@nestjs/swagger';
import { IsIn } from 'class-validator';

/**
 * Cuerpo de `PUT /intentos/{intentoId}/respuestas/{preguntaId}`
 * (CONTRATOS.md 8.3).
 */
export class RegistrarRespuestaDto {
  @ApiProperty({ example: 'B', enum: ['A', 'B', 'C', 'D'] })
  @IsIn(['A', 'B', 'C', 'D'])
  letraSeleccionada!: string;
}
