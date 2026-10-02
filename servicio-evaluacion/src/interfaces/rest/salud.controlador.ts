import { Controller, Get } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import { ConfiguracionServicio } from '../../infraestructura/configuracion/configuracion.servicio';
import { RutaPublica } from './decoradores/ruta-publica.decorator';

/** Forma exacta de la respuesta de `GET /salud` (CONTRATOS.md 5.1). */
interface RespuestaSalud {
  readonly estado: 'OK';
  readonly servicio: string;
}

/**
 * `GET /salud` (CONTRATOS.md 5.1): fuera de `/api/v1`, sin encabezados de
 * identidad.
 */
@ApiTags('Salud')
@RutaPublica()
@Controller('salud')
export class SaludControlador {
  constructor(private readonly configuracion: ConfiguracionServicio) {}

  @Get()
  @ApiOperation({ summary: 'Comprueba que el servicio esta en linea.' })
  public obtenerSalud(): RespuestaSalud {
    return { estado: 'OK', servicio: this.configuracion.nombreServicio };
  }
}
