import { Body, Controller, Get, HttpCode, Param, Post, Put, Res } from '@nestjs/common';
import { ApiHeader, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { Response } from 'express';
import { UsuarioActual } from '../../aplicacion/compartido/usuario-actual';
import { FinalizarIntentoCasoUso } from '../../aplicacion/intentos/finalizar-intento.caso-uso';
import { IniciarIntentoCasoUso } from '../../aplicacion/intentos/iniciar-intento.caso-uso';
import { ObtenerIntentoCasoUso } from '../../aplicacion/intentos/obtener-intento.caso-uso';
import { RegistrarRespuestaCasoUso } from '../../aplicacion/intentos/registrar-respuesta.caso-uso';
import { normalizarUuid } from '../../dominio/compartido/uuid';
import { SolicitudInvalidaExcepcion } from '../../dominio/excepciones/solicitud-invalida.excepcion';
import { Usuario } from './decoradores/usuario.decorator';
import { RegistrarRespuestaDto } from './dto/registrar-respuesta.dto';

/**
 * Controlador REST de IntentoDeSimulacro (CONTRATOS.md 8.3). Usa rutas
 * explicitas (no un unico prefijo de controlador) porque el primer
 * endpoint cuelga de `/simulacros/{simulacroId}` y los demas de
 * `/intentos/{intentoId}`.
 */
@ApiTags('Intentos')
@ApiHeader({ name: 'X-Usuario-Id', required: true, description: 'UUID del usuario (CONTRATOS.md 4.1).' })
@ApiHeader({ name: 'X-Roles', required: true, description: 'Roles separados por comas (CONTRATOS.md 4.1).' })
@ApiHeader({ name: 'X-Id-Correlacion', required: false, description: 'Se genera si no llega (CONTRATOS.md 4.1).' })
@Controller()
export class IntentosControlador {
  constructor(
    private readonly iniciarIntento: IniciarIntentoCasoUso,
    private readonly registrarRespuesta: RegistrarRespuestaCasoUso,
    private readonly finalizarIntento: FinalizarIntentoCasoUso,
    private readonly obtenerIntento: ObtenerIntentoCasoUso,
  ) {}

  @Post('simulacros/:simulacroId/intentos')
  @HttpCode(201)
  @ApiOperation({ summary: 'Inicia un intento sobre un simulacro (CU-14).', description: 'Rol requerido: ESTUDIANTE.' })
  @ApiResponse({ status: 201, description: 'Intento iniciado, EN_CURSO.' })
  @ApiResponse({ status: 404, description: 'SIMULACRO_NO_ENCONTRADO.' })
  public async iniciar(
    @Usuario() usuario: UsuarioActual,
    @Param('simulacroId') simulacroId: string,
    @Res({ passthrough: true }) respuesta: Response,
  ) {
    const intento = await this.iniciarIntento.ejecutar({ usuario, simulacroId });
    respuesta.location(`/api/v1/intentos/${intento.intentoId}`);
    return intento;
  }

  @Put('intentos/:intentoId/respuestas/:preguntaId')
  @ApiOperation({ summary: 'Registra la respuesta a una pregunta del intento (CU-14).', description: 'Rol requerido: ESTUDIANTE (dueno).' })
  @ApiResponse({ status: 200, description: 'Respuesta registrada.' })
  @ApiResponse({ status: 404, description: 'INTENTO_NO_ENCONTRADO.' })
  @ApiResponse({ status: 409, description: 'INTENTO_FINALIZADO.' })
  @ApiResponse({ status: 422, description: 'PREGUNTA_NO_PERTENECE_AL_SIMULACRO.' })
  public async responder(
    @Usuario() usuario: UsuarioActual,
    @Param('intentoId') intentoId: string,
    @Param('preguntaId') preguntaIdCrudo: string,
    @Body() cuerpo: RegistrarRespuestaDto,
  ) {
    // Un UUID canonico en mayusculas se acepta y se normaliza a minusculas
    // (CONTRATOS.md seccion 4). No hay un value object PreguntaId en este
    // servicio (la pregunta la gestiona Editorial); se normaliza aqui,
    // en el borde HTTP.
    const preguntaId = normalizarUuid(preguntaIdCrudo);
    if (!preguntaId) {
      throw new SolicitudInvalidaExcepcion('El preguntaId debe ser un UUID valido.');
    }
    return this.registrarRespuesta.ejecutar({
      usuario,
      intentoId,
      preguntaId,
      letraSeleccionada: cuerpo.letraSeleccionada,
    });
  }

  @Post('intentos/:intentoId/finalizacion')
  @HttpCode(200)
  @ApiOperation({ summary: 'Finaliza y califica el intento en la misma operacion (CU-14 + CU-15).', description: 'Rol requerido: ESTUDIANTE (dueno).' })
  @ApiResponse({ status: 200, description: 'Intento finalizado y calificado (publica IntentoDeSimulacroCalificado).' })
  @ApiResponse({ status: 409, description: 'INTENTO_FINALIZADO (ya estaba finalizado o calificado antes de esta peticion).' })
  public async finalizar(@Usuario() usuario: UsuarioActual, @Param('intentoId') intentoId: string) {
    return this.finalizarIntento.ejecutar({ usuario, intentoId });
  }

  @Get('intentos/:intentoId')
  @ApiOperation({ summary: 'Obtiene un intento por id.', description: 'ESTUDIANTE: solo el suyo. DOCENTE: cualquiera, en solo lectura.' })
  @ApiResponse({ status: 200, description: 'IntentoRespuesta.' })
  @ApiResponse({ status: 404, description: 'INTENTO_NO_ENCONTRADO.' })
  @ApiResponse({ status: 403, description: 'ACCESO_DENEGADO.' })
  public async obtener(@Usuario() usuario: UsuarioActual, @Param('intentoId') intentoId: string) {
    return this.obtenerIntento.ejecutar({ usuario, intentoId });
  }
}
