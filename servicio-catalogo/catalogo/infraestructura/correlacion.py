"""Identificador de correlación de la petición en curso (CONTRATOS.md 4.1).

``X-Id-Correlacion`` (REST) y ``x-id-correlacion`` (metadato gRPC) viajan en una variable de
contexto para que cada línea de log y cada respuesta de error lo incluyan sin pasarlo de
mano en mano por todas las funciones.
"""

from __future__ import annotations

import logging
from collections.abc import Iterator
from contextlib import contextmanager
from contextvars import ContextVar
from uuid import UUID, uuid4

registro = logging.getLogger(__name__)

ENCABEZADO_CORRELACION = "X-Id-Correlacion"
"""Nombre del encabezado HTTP de correlación."""

METADATO_CORRELACION = "x-id-correlacion"
"""Nombre del metadato gRPC de correlación."""

_ID_CORRELACION_ACTUAL: ContextVar[str | None] = ContextVar("id_correlacion", default=None)
ID_CORRELACION_PROCESO = str(uuid4())
"""Correlación de las líneas que no pertenecen a ninguna petición (arranque, tareas de fondo)."""


def generar_id_correlacion() -> str:
    """Genera un identificador de correlación nuevo (UUID v4 en minúsculas).

    Returns:
        El identificador.
    """
    return str(uuid4())


def normalizar_id_correlacion(recibido: str | None) -> str:
    """Devuelve el identificador recibido si es un UUID válido; si no, genera uno nuevo.

    CONTRATOS.md 4.1: ``X-Id-Correlacion`` es un UUID y, si no llega, el servicio lo genera.
    Un valor que no es UUID no se rechaza (es solo un dato de trazabilidad): se reemplaza
    por uno nuevo y se registra un aviso (CONTRATOS.md 4.1, v1.10).

    Args:
        recibido: Valor del encabezado o metadato, o ``None`` si no llegó.

    Returns:
        Un UUID en texto minúsculo.
    """
    # CONTRATOS 4.1 (v1.10): si no llega se genera uno; si llega con formato inválido se
    # genera uno nuevo y se registra un aviso. Nunca es un error de la petición.
    if not recibido:
        return generar_id_correlacion()
    candidato = recibido.strip().lower()
    try:
        if str(UUID(candidato)) == candidato:
            return candidato
    except ValueError:
        pass
    nuevo = generar_id_correlacion()
    registro.warning(
        "Correlación recibida con formato inválido (%.40r); se generó %s", recibido, nuevo
    )
    return nuevo


def obtener_id_correlacion() -> str:
    """Devuelve la correlación de la petición en curso.

    Returns:
        La correlación activa, o la del proceso si no hay petición en curso.
    """
    return _ID_CORRELACION_ACTUAL.get() or ID_CORRELACION_PROCESO


@contextmanager
def correlacion_activa(id_correlacion: str) -> Iterator[str]:
    """Activa una correlación mientras dura el bloque ``with``.

    Args:
        id_correlacion: Identificador a activar.

    Yields:
        El mismo identificador.
    """
    marca = _ID_CORRELACION_ACTUAL.set(id_correlacion)
    try:
        yield id_correlacion
    finally:
        _ID_CORRELACION_ACTUAL.reset(marca)
