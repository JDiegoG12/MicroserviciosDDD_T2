"""Dependencias de FastAPI: ejecutor de casos de uso e identidad del usuario (CONTRATOS.md 4.1)."""

from __future__ import annotations

from typing import Annotated
from uuid import UUID

from fastapi import Header, Request

from catalogo.aplicacion.excepciones import NoAutenticadoExcepcion
from catalogo.aplicacion.puertos.salida.ejecutor_transaccional_puerto import (
    EjecutorTransaccionalPuerto,
)
from catalogo.aplicacion.seguridad.usuario_actual import Rol, UsuarioActual
from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion


def obtener_ejecutor(solicitud: Request) -> EjecutorTransaccionalPuerto:
    """Entrega el ejecutor transaccional con el que se armó la aplicación.

    Args:
        solicitud: Petición en curso.

    Returns:
        El ejecutor guardado en ``app.state``.
    """
    return solicitud.app.state.ejecutor


def _uuid_canonico(texto: str) -> str | None:
    """Devuelve el UUID en minúsculas si el texto es un UUID de forma canónica, o ``None``."""
    candidato = texto.strip().lower()
    try:
        return candidato if str(UUID(candidato)) == candidato else None
    except ValueError:
        return None


def _leer_roles(encabezado: str) -> frozenset[Rol]:
    """Interpreta ``X-Roles``: lista separada por comas, sin espacios significativos.

    Args:
        encabezado: Valor del encabezado.

    Returns:
        Los roles.

    Raises:
        NoAutenticadoExcepcion: Si la lista queda vacía (CONTRATOS.md 4.1, v1.9).
        SolicitudInvalidaExcepcion: Si algún rol no es uno de los cinco válidos.
    """
    nombres = [parte.strip() for parte in encabezado.split(",") if parte.strip()]
    if not nombres:
        raise NoAutenticadoExcepcion("El encabezado X-Roles llegó vacío.")
    roles = set()
    for nombre in nombres:
        try:
            roles.add(Rol(nombre))
        except ValueError:
            validos = ", ".join(rol.value for rol in Rol)
            raise SolicitudInvalidaExcepcion(
                f"El rol '{nombre}' no es válido. Roles válidos: {validos}."
            ) from None
    return frozenset(roles)


def obtener_usuario_actual(
    x_usuario_id: Annotated[
        str | None,
        Header(
            alias="X-Usuario-Id",
            description="Obligatorio. UUID del usuario que llama (en producción lo pondría un "
            "API Gateway). Falta → 401 `NO_AUTENTICADO`; no es un UUID → 400.",
            examples=["11111111-1111-4111-8111-000000000001"],
        ),
    ] = None,
    x_roles: Annotated[
        str | None,
        Header(
            alias="X-Roles",
            description="Obligatorio. Roles separados por comas: `ADMINISTRADOR`, `AUTOR`, "
            "`REVISOR`, `DOCENTE`, `ESTUDIANTE`. Falta o vacío → 401 `NO_AUTENTICADO`; un rol "
            "desconocido → 400. Las escrituras del catálogo exigen `ADMINISTRADOR`.",
            examples=["ADMINISTRADOR"],
        ),
    ] = None,
    x_id_correlacion: Annotated[
        str | None,
        Header(
            alias="X-Id-Correlacion",
            description="Opcional. UUID de correlación; si no llega (o no es un UUID) el servicio "
            "genera uno. Se devuelve en la respuesta y aparece en todas las líneas del log.",
            examples=["0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b"],
        ),
    ] = None,
) -> UsuarioActual:
    """Arma ``UsuarioActual`` desde los encabezados de identidad (CONTRATOS.md 4.1).

    ``X-Id-Correlacion`` se declara aquí solo para que Swagger lo documente en cada
    endpoint: lo procesa ``MiddlewareCorrelacion``.

    Args:
        x_usuario_id: Encabezado ``X-Usuario-Id``.
        x_roles: Encabezado ``X-Roles``.
        x_id_correlacion: Encabezado ``X-Id-Correlacion`` (solo documentación).

    Returns:
        El usuario con sus roles.

    Raises:
        NoAutenticadoExcepcion: Si falta ``X-Usuario-Id`` o ``X-Roles``, o este llega vacío.
        SolicitudInvalidaExcepcion: Si ``X-Usuario-Id`` no es un UUID o hay un rol inválido.
    """
    if not x_usuario_id or x_roles is None:
        raise NoAutenticadoExcepcion("Faltan los encabezados X-Usuario-Id y X-Roles.")
    roles = _leer_roles(x_roles)
    usuario_id = _uuid_canonico(x_usuario_id)
    if usuario_id is None:
        raise SolicitudInvalidaExcepcion("El encabezado X-Usuario-Id no es un UUID bien formado.")
    return UsuarioActual(usuario_id=usuario_id, roles=roles)
