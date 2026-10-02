import { ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { IsInt, IsOptional, Min } from 'class-validator';

/**
 * Parametros de query comunes de paginacion (CONTRATOS.md 5.1): `pagina`
 * desde 0, `tamano` por defecto 20. La validacion de rango exacta
 * (`tamano` entre 1 y 100) la hace `validarPaginacion` en `aplicacion`,
 * para no duplicar la regla.
 */
export class PaginacionDto {
  @ApiPropertyOptional({ example: 0, minimum: 0 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  pagina?: number;

  @ApiPropertyOptional({ example: 20, minimum: 1 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  tamano?: number;
}
