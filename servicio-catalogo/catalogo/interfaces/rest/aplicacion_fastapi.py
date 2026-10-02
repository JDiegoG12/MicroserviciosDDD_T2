"""Fábrica de la aplicación FastAPI del catálogo."""

from __future__ import annotations

from typing import Any

from fastapi import FastAPI

from catalogo.aplicacion.puertos.salida.ejecutor_transaccional_puerto import (
    EjecutorTransaccionalPuerto,
)
from catalogo.interfaces.rest.documentacion import ajustar_openapi
from catalogo.interfaces.rest.errores import instalar_manejadores_de_errores
from catalogo.interfaces.rest.middleware_correlacion import MiddlewareCorrelacion
from catalogo.interfaces.rest.routers import competencias, salud

DESCRIPCION_API = """
API del **Catálogo Académico** del Banco de Preguntas Saber Pro: competencias, temas y subtemas.

* Prefijo `/api/v1`. La identidad llega en los encabezados `X-Usuario-Id` y `X-Roles`.
* Las escrituras exigen el rol `ADMINISTRADOR`; las lecturas, cualquier rol.
* Errores en formato `application/problem+json` (RFC 7807) con el campo `codigo`.
* La validación de clasificaciones para otros servicios se expone por **gRPC** (puerto 50051).
"""


def crear_aplicacion(ejecutor: EjecutorTransaccionalPuerto) -> FastAPI:
    """Crea la aplicación FastAPI con sus routers, errores y middleware.

    Swagger queda en ``/docs`` y el OpenAPI JSON en ``/openapi.json``.

    Args:
        ejecutor: Ejecutor transaccional con el que los endpoints corren los casos de uso.
            En las pruebas de la API se entrega uno en memoria.

    Returns:
        La aplicación lista para servir.
    """
    aplicacion = FastAPI(
        title="servicio-catalogo · Catálogo Académico",
        version="0.2.0",
        description=DESCRIPCION_API,
        docs_url="/docs",
        openapi_url="/openapi.json",
        redoc_url=None,
    )
    aplicacion.state.ejecutor = ejecutor
    aplicacion.include_router(salud.router)
    aplicacion.include_router(competencias.router)
    instalar_manejadores_de_errores(aplicacion)
    aplicacion.add_middleware(MiddlewareCorrelacion)

    esquema_en_cache: dict[str, Any] = {}

    def openapi_personalizado() -> dict[str, Any]:
        """Genera el OpenAPI una sola vez y lo ajusta a CONTRATOS.md 5.2 y 5.3."""
        if not esquema_en_cache:
            esquema_en_cache.update(ajustar_openapi(FastAPI.openapi(aplicacion)))
        return esquema_en_cache

    aplicacion.openapi = openapi_personalizado  # type: ignore[method-assign]
    return aplicacion
