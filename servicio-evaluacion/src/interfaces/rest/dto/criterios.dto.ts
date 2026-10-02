import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsEnum, IsUUID } from 'class-validator';
import { NivelDificultad } from '../../../dominio/compartido/nivel-dificultad';
import { NormalizarUuidLista } from './normalizar-uuid.transformador';

/**
 * Criterios de generacion de un Simulacro (CONTRATOS.md 8.3). Cualquier
 * lista puede venir vacia: una lista vacia no filtra.
 */
export class CriteriosDto {
  @ApiProperty({ type: [String], format: 'uuid', example: ['22222222-2222-4222-8222-000000000101'] })
  @IsArray()
  @NormalizarUuidLista()
  @IsUUID('4', { each: true })
  competenciaIds!: string[];

  @ApiProperty({ type: [String], format: 'uuid', example: [] })
  @IsArray()
  @NormalizarUuidLista()
  @IsUUID('4', { each: true })
  temaIds!: string[];

  @ApiProperty({ type: [String], format: 'uuid', example: [] })
  @IsArray()
  @NormalizarUuidLista()
  @IsUUID('4', { each: true })
  subtemaIds!: string[];

  @ApiProperty({ enum: NivelDificultad, isArray: true, example: ['BAJO', 'MEDIO'] })
  @IsArray()
  @IsEnum(NivelDificultad, { each: true })
  nivelesDificultad!: NivelDificultad[];
}
