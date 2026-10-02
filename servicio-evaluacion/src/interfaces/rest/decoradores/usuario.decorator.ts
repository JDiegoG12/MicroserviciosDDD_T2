import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { Request } from 'express';
import { UsuarioActual } from '../../../aplicacion/compartido/usuario-actual';

/**
 * Inyecta el `UsuarioActual` que `IdentidadGuardia` ya armo y dejo en
 * `request.usuarioActual`, para que los controladores lo pasen tal cual al
 * comando o consulta del caso de uso correspondiente.
 */
export const Usuario = createParamDecorator((_datos: unknown, contexto: ExecutionContext): UsuarioActual => {
  const peticion = contexto.switchToHttp().getRequest<Request & { usuarioActual?: UsuarioActual }>();
  return peticion.usuarioActual as UsuarioActual;
});
