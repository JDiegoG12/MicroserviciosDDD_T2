import { SetMetadata } from '@nestjs/common';

/** Clave de metadata usada por `IdentidadGuardia` para saltar la autenticacion. */
export const CLAVE_RUTA_PUBLICA = 'rutaPublica';

/**
 * Marca un controlador o un metodo como publico: no exige los encabezados
 * de identidad `X-Usuario-Id` ni `X-Roles` (CONTRATOS.md 4.1: "excepto
 * /salud y /docs").
 */
export const RutaPublica = (): MethodDecorator & ClassDecorator => SetMetadata(CLAVE_RUTA_PUBLICA, true);
