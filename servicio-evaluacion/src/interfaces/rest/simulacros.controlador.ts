import { Body, Controller, Get, HttpCode, Param, Post, Query, Res } from '@nestjs/common';
import { ApiHeader, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { Response } from 'express';
import { DefinirSimulacroCasoUso } from '../../aplicacion/simulacros/definir-simulacro.caso-uso';
import { ListarSimulacrosCasoUso } from '../../aplicacion/simulacros/listar-simulacros.caso-uso';
import { ObtenerSimulacroCasoUso } from '../../aplicacion/simulacros/obtener-simulacro.caso-uso';
import { UsuarioActual } from '../../aplicacion/compartido/usuario-actual';
import { Usuario } from './decoradores/usuario.decorator';
import { DefinirSimulacroDto } from './dto/definir-simulacro.dto';
import { PaginacionDto } from './dto/paginacion.dto';

/**
 * Controlador REST de Simulacro (CONTRATOS.md 8.3). Solo valida la forma
 * de la solicitud, arma el comando o la consulta y llama al caso de uso
 * correspondiente: ninguna regla de negocio vive aqui (CONTRATOS.md 3.3).
 */
@ApiTags('Simulacros')
@ApiHeader({ name: 'X-Usuario-Id', required: true, description: 'UUID del usuario (CONTRATOS.md 4.1).' })
@ApiHeader({ name: 'X-Roles', required: true, description: 'Roles separados por comas (CONTRATOS.md 4.1).' })
@ApiHeader({ name: 'X-Id-Correlacion', required: false, description: 'Se genera si no llega (CONTRATOS.md 4.1).' })
@Controller('simulacros')
export class SimulacrosControlador {
  constructor(
    private readonly definirSimulacro: DefinirSimulacroCasoUso,
    private readonly listarSimulacros: ListarSimulacrosCasoUso,
    private readonly obtenerSimulacro: ObtenerSimulacroCasoUso,
  ) {}

  @Post()
  @HttpCode(201)
  @ApiOperation({ summary: 'Define un simulacro nuevo (CU-13).', description: 'Rol requerido: DOCENTE.' })
  @ApiResponse({ status: 201, description: 'Simulacro definido.' })
  @ApiResponse({ status: 422, description: 'PREGUNTAS_INSUFICIENTES, PREGUNTA_NO_PUBLICADA, PREGUNTA_DUPLICADA_EN_SIMULACRO o DURACION_INVALIDA.' })
  public async definir(
    @Usuario() usuario: UsuarioActual,
    @Res({ passthrough: true }) respuesta: Response,
    @Body() body: DefinirSimulacroDto,
  ) {
    const simulacro = await this.definirSimulacro.ejecutar({
      usuario,
      nombre: body.nombre,
      criterios: body.criterios,
      cantidadPreguntas: body.cantidadPreguntas,
      duracionMaximaMinutos: body.duracionMaximaMinutos,
    });
    respuesta.location(`/api/v1/simulacros/${simulacro.simulacroId}`);
    return simulacro;
  }

  @Get()
  @ApiOperation({ summary: 'Lista los simulacros definidos, paginados.', description: 'Roles: DOCENTE, ESTUDIANTE.' })
  @ApiResponse({ status: 200, description: 'Pagina de SimulacroResumen.' })
  public async listar(@Usuario() usuario: UsuarioActual, @Query() paginacion: PaginacionDto) {
    return this.listarSimulacros.ejecutar({
      usuario,
      pagina: paginacion.pagina,
      tamano: paginacion.tamano,
    });
  }

  @Get(':simulacroId')
  @ApiOperation({ summary: 'Obtiene un simulacro por id, sin letraCorrecta.', description: 'Roles: DOCENTE, ESTUDIANTE.' })
  @ApiResponse({ status: 200, description: 'SimulacroRespuesta.' })
  @ApiResponse({ status: 404, description: 'SIMULACRO_NO_ENCONTRADO.' })
  public async obtener(@Usuario() usuario: UsuarioActual, @Param('simulacroId') simulacroId: string) {
    return this.obtenerSimulacro.ejecutar({ usuario, simulacroId });
  }
}
