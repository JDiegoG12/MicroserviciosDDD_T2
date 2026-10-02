import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Request } from 'express';
import { Rol, UsuarioActual } from '../../../aplicacion/compartido/usuario-actual';
import { esUuidValido } from '../../../dominio/compartido/uuid';
import { SolicitudInvalidaExcepcion } from '../../../dominio/excepciones/solicitud-invalida.excepcion';
import { CLAVE_RUTA_PUBLICA } from '../decoradores/ruta-publica.decorator';
import { NoAutenticadoExcepcion } from '../excepciones/no-autenticado.excepcion';

const ROLES_VALIDOS = new Set<string>(Object.values(Rol));

/**
 * Arma `UsuarioActual` a partir de los encabezados `X-Usuario-Id` y
 * `X-Roles` (CONTRATOS.md 4.1) y lo deja en `request.usuarioActual` para
 * que `@Usuario()` lo inyecte en el controlador.
 *
 * La verificacion de **rol** la hace cada caso de uso
 * (`UsuarioActual.exigirAlgunRol`, en `aplicacion`); esta guardia solo
 * construye la identidad y rechaza lo que CONTRATOS.md exige rechazar
 * antes de llegar al caso de uso.
 *
 * @throws NoAutenticadoExcepcion Si falta `X-Usuario-Id` o `X-Roles` (401).
 * @throws SolicitudInvalidaExcepcion Si `X-Usuario-Id` no es un UUID, o si
 * `X-Roles` trae un valor que no es uno de los cinco roles validos (400).
 */
@Injectable()
export class IdentidadGuardia implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  public canActivate(contexto: ExecutionContext): boolean {
    const esPublica = this.reflector.getAllAndOverride<boolean>(CLAVE_RUTA_PUBLICA, [
      contexto.getHandler(),
      contexto.getClass(),
    ]);
    if (esPublica) {
      return true;
    }

    const peticion = contexto.switchToHttp().getRequest<Request & { usuarioActual?: UsuarioActual }>();
    const usuarioId = peticion.header('X-Usuario-Id');
    const rolesCrudos = peticion.header('X-Roles');

    if (!usuarioId || !rolesCrudos) {
      throw new NoAutenticadoExcepcion('Faltan los encabezados X-Usuario-Id y/o X-Roles (CONTRATOS.md 4.1).');
    }
    if (!esUuidValido(usuarioId)) {
      throw new SolicitudInvalidaExcepcion('El encabezado X-Usuario-Id debe ser un UUID valido.');
    }

    const roles = rolesCrudos.split(',').map((rol: string) => rol.trim());
    for (const rol of roles) {
      if (!ROLES_VALIDOS.has(rol)) {
        throw new SolicitudInvalidaExcepcion(
          `El encabezado X-Roles trae un rol invalido: "${rol}". Los roles validos son ${[...ROLES_VALIDOS].join(', ')}.`,
        );
      }
    }

    peticion.usuarioActual = new UsuarioActual(usuarioId, roles as Rol[]);
    return true;
  }
}
