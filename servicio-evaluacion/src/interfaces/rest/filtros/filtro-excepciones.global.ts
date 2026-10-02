import { ArgumentsHost, Catch, ExceptionFilter, HttpException } from '@nestjs/common';
import { Request, Response } from 'express';
import { obtenerIdCorrelacionActual } from '../../../infraestructura/correlacion/contexto-correlacion';
import { registrar } from '../../../infraestructura/observabilidad/registrador';
import { ErrorDeCampo, SolicitudInvalidaHttpExcepcion } from '../excepciones/solicitud-invalida-http.excepcion';
import { MAPA_CODIGO_A_HTTP, MAPA_CODIGO_A_TITULO } from './mapa-codigo-http';

interface ExcepcionConCodigo {
  readonly codigo?: unknown;
  readonly message?: unknown;
}

/**
 * Filtro global de excepciones: traduce cualquier excepcion lanzada por el
 * dominio, la aplicacion o la propia capa REST al formato unico
 * `application/problem+json` de CONTRATOS.md 5.2, usando el mapa de la
 * seccion 5.3 y 8.3.
 *
 * Nunca expone trazas (CONTRATOS.md 5.2): un error sin `codigo` reconocido
 * se responde como 500 `ERROR_INTERNO` con un mensaje generico, y la causa
 * real solo queda en el log (con su `idCorrelacion`).
 */
@Catch()
export class FiltroExcepcionesGlobal implements ExceptionFilter {
  public catch(excepcion: unknown, host: ArgumentsHost): void {
    const contexto = host.switchToHttp();
    const respuesta = contexto.getResponse<Response>();
    const peticion = contexto.getRequest<Request>();
    const idCorrelacion = obtenerIdCorrelacionActual();

    const { status, codigo, detalle, errores } = this.interpretar(excepcion);

    if (status >= 500) {
      registrar('error', 'Error inesperado al atender la peticion.', {
        error: excepcion instanceof Error ? excepcion.message : String(excepcion),
        stack: excepcion instanceof Error ? excepcion.stack : undefined,
        ruta: peticion.originalUrl,
      });
    }

    respuesta
      .status(status)
      .type('application/problem+json')
      .json({
        type: `https://banco-preguntas/errores/${codigo}`,
        title: MAPA_CODIGO_A_TITULO[codigo] ?? 'Error',
        status,
        detail: detalle,
        instance: peticion.originalUrl,
        codigo,
        idCorrelacion,
        ...(errores && errores.length > 0 ? { errores } : {}),
      });
  }

  private interpretar(excepcion: unknown): {
    status: number;
    codigo: string;
    detalle: string;
    errores?: readonly ErrorDeCampo[];
  } {
    if (excepcion instanceof SolicitudInvalidaHttpExcepcion) {
      return {
        status: 400,
        codigo: excepcion.codigo,
        detalle: excepcion.message,
        errores: excepcion.errores,
      };
    }

    const conCodigo = excepcion as ExcepcionConCodigo;
    if (typeof conCodigo?.codigo === 'string' && conCodigo.codigo in MAPA_CODIGO_A_HTTP) {
      const codigo = conCodigo.codigo;
      return {
        status: MAPA_CODIGO_A_HTTP[codigo],
        codigo,
        detalle: typeof conCodigo.message === 'string' ? conCodigo.message : 'Error de negocio.',
      };
    }

    // Excepciones propias de NestJS que no pasaron por nuestras capas
    // (por ejemplo, una ruta que no existe): se traduce su status, pero
    // siempre con un codigo y un detalle genericos, nunca con la traza.
    if (excepcion instanceof HttpException) {
      const status = excepcion.getStatus();
      return {
        status,
        codigo: status === 404 ? 'RECURSO_NO_ENCONTRADO' : 'ERROR_INTERNO',
        detalle: 'La solicitud no pudo procesarse.',
      };
    }

    return { status: 500, codigo: 'ERROR_INTERNO', detalle: 'Ocurrio un error inesperado.' };
  }
}
