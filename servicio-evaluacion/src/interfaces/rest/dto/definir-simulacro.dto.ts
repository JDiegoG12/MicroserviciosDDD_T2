import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { IsInt, IsNotEmpty, IsString, Min, ValidateNested } from 'class-validator';
// CONTRATOS 5.3: 400 es para datos mal formados (tipo incorrecto, JSON
// invalido); 422 es para datos bien formados que violan una regla de
// negocio. Un entero no positivo esta bien formado, asi que
// duracionMaximaMinutos solo valida @IsInt() aqui (sin @Min): el valor
// llega intacto al dominio, donde INV-28 lo rechaza con 422
// DURACION_INVALIDA. cantidadPreguntas si usa @Min(1) porque su propio
// rechazo de dominio (EnsambladorSimulacroServicio) tambien es
// SOLICITUD_INVALIDA (400): ahi no hay conflicto de codigo.
import { CriteriosDto } from './criterios.dto';

/**
 * Cuerpo de `POST /simulacros` (CONTRATOS.md 8.3, CU-13).
 */
export class DefinirSimulacroDto {
  @ApiProperty({ example: 'Simulacro cuantitativo 1' })
  @IsString()
  @IsNotEmpty()
  nombre!: string;

  @ApiProperty({ type: CriteriosDto })
  @ValidateNested()
  @Type(() => CriteriosDto)
  criterios!: CriteriosDto;

  @ApiProperty({ example: 5, minimum: 1 })
  @IsInt()
  @Min(1)
  cantidadPreguntas!: number;

  @ApiProperty({ example: 30, minimum: 1, description: 'Debe ser entero; un valor no positivo responde 422 DURACION_INVALIDA (INV-28).' })
  @IsInt()
  duracionMaximaMinutos!: number;
}
