"""Middleware de correlación y registro de peticiones (CONTRATOS.md 4.1).

Es un middleware ASGI puro (no ``BaseHTTPMiddleware``) para controlar el ciclo completo:

* lee ``X-Id-Correlacion`` o genera uno y lo deja activo para todo el log de la petición;
* lo devuelve en el encabezado de la respuesta;
* escribe una línea de log por petición;
* rechaza con 415 ``TIPO_DE_CONTENIDO_NO_SOPORTADO`` los cuerpos que no son JSON
  (CONTRATOS 5.3, v1.10);
* convierte un error inesperado en 500 ``ERROR_INTERNO`` sin exponer la traza.
"""

from __future__ import annotations

import logging
import time
from typing import Any

from starlette.datastructures import Headers, MutableHeaders
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from catalogo.infraestructura.correlacion import (
    ENCABEZADO_CORRELACION,
    correlacion_activa,
    normalizar_id_correlacion,
)
from catalogo.interfaces.rest.errores import crear_problema

registro = logging.getLogger("catalogo.http")
RUTAS_SILENCIOSAS = ("/salud",)
"""Rutas que se registran en nivel DEBUG para no llenar el log con el *healthcheck*."""
METODOS_CON_CUERPO = ("POST", "PUT", "PATCH")
"""Métodos cuyo cuerpo debe ser JSON; el resto no se revisa."""


def _tiene_tipo_de_contenido_no_soportado(scope: Scope) -> bool:
    """Indica si la petición trae un cuerpo que no declara ser JSON.

    Solo se revisan ``POST``, ``PUT`` y ``PATCH`` con cuerpo (``Content-Length`` mayor que
    cero o ``Transfer-Encoding``). Se acepta ``application/json`` y los ``+json``, con o sin
    ``charset``. Una petición sin cuerpo no se rechaza aquí: la validación responde 400.

    Args:
        scope: Alcance de la conexión ASGI.

    Returns:
        ``True`` si hay cuerpo y su tipo de contenido no es JSON (o no se declara).
    """
    if scope["method"] not in METODOS_CON_CUERPO:
        return False
    encabezados = Headers(scope=scope)
    longitud = encabezados.get("content-length", "0")
    hay_cuerpo = (longitud.isdigit() and int(longitud) > 0) or "transfer-encoding" in encabezados
    if not hay_cuerpo:
        return False
    tipo = encabezados.get("content-type", "").split(";")[0].strip().lower()
    return not (tipo == "application/json" or tipo.endswith("+json"))


class MiddlewareCorrelacion:
    """Activa la correlación de cada petición HTTP y registra su resultado."""

    def __init__(self, aplicacion: ASGIApp) -> None:
        """Crea el middleware.

        Args:
            aplicacion: Aplicación ASGI envuelta.
        """
        self._aplicacion = aplicacion

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        """Atiende una petición ASGI.

        Args:
            scope: Alcance de la conexión.
            receive: Canal de entrada.
            send: Canal de salida.
        """
        if scope["type"] != "http":
            await self._aplicacion(scope, receive, send)
            return

        recibido = Headers(scope=scope).get(ENCABEZADO_CORRELACION)
        with correlacion_activa(normalizar_id_correlacion(recibido)) as id_correlacion:
            inicio = time.perf_counter()
            estado: dict[str, Any] = {"codigo": None}

            async def enviar_con_correlacion(mensaje: Message) -> None:
                """Agrega ``X-Id-Correlacion`` al encabezado de la respuesta y anota su estado."""
                if mensaje["type"] == "http.response.start":
                    estado["codigo"] = mensaje["status"]
                    MutableHeaders(scope=mensaje)[ENCABEZADO_CORRELACION] = id_correlacion
                await send(mensaje)

            try:
                if _tiene_tipo_de_contenido_no_soportado(scope):
                    respuesta = crear_problema(
                        scope["path"],
                        "TIPO_DE_CONTENIDO_NO_SOPORTADO",
                        "El cuerpo debe enviarse como application/json.",
                    )
                    await respuesta(scope, receive, enviar_con_correlacion)
                else:
                    await self._aplicacion(scope, receive, enviar_con_correlacion)
            except Exception:
                registro.exception("Error inesperado atendiendo %s %s", scope["method"], scope["path"])
                if estado["codigo"] is not None:
                    raise
                respuesta = crear_problema(
                    scope["path"], "ERROR_INTERNO", "Ocurrió un error inesperado."
                )
                await respuesta(scope, receive, enviar_con_correlacion)
            finally:
                duracion_ms = (time.perf_counter() - inicio) * 1000
                nivel = logging.DEBUG if scope["path"] in RUTAS_SILENCIOSAS else logging.INFO
                registro.log(
                    nivel,
                    "%s %s -> %s (%.0f ms)",
                    scope["method"],
                    scope["path"],
                    estado["codigo"],
                    duracion_ms,
                )
