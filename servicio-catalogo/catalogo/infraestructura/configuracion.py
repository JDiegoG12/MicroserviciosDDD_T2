"""Configuración del servicio desde variables de entorno (CONTRATOS.md 9.2).

Los valores por defecto son los "locales" de la tabla 9.2, para poder ejecutar el servicio
desde el IDE con la base de datos en Docker. No hay credenciales en el código más allá de
las de desarrollo local de esa tabla; en Docker llegan por ``CATALOGO_BD_URL``.
"""

from __future__ import annotations

import os
from collections.abc import Mapping
from dataclasses import dataclass

URL_BD_LOCAL_POR_DEFECTO = "postgresql+psycopg://catalogo:catalogo@localhost:5434/catalogo"


class ConfiguracionInvalidaError(Exception):
    """Una variable de entorno tiene un valor que no se puede usar."""


@dataclass(frozen=True, slots=True)
class Configuracion:
    """Valores de configuración del servicio.

    Attributes:
        puerto_http: Puerto REST (``CATALOGO_PUERTO_HTTP``, por defecto 8082).
        puerto_grpc: Puerto gRPC (``CATALOGO_PUERTO_GRPC``, por defecto 50051).
        url_base_datos: URL SQLAlchemy de PostgreSQL (``CATALOGO_BD_URL``).
    """

    puerto_http: int = 8082
    puerto_grpc: int = 50051
    url_base_datos: str = URL_BD_LOCAL_POR_DEFECTO

    @classmethod
    def desde_entorno(cls, entorno: Mapping[str, str] | None = None) -> Configuracion:
        """Lee la configuración de las variables de entorno.

        Args:
            entorno: Variables a usar; por defecto ``os.environ``.

        Returns:
            La configuración, con los valores por defecto locales donde falte una variable.

        Raises:
            ConfiguracionInvalidaError: Si un puerto no es un entero entre 1 y 65535 o la URL
                de la base de datos no usa el controlador ``postgresql+psycopg``.
        """
        entorno = os.environ if entorno is None else entorno
        configuracion = cls(
            puerto_http=_leer_puerto(entorno, "CATALOGO_PUERTO_HTTP", 8082),
            puerto_grpc=_leer_puerto(entorno, "CATALOGO_PUERTO_GRPC", 50051),
            url_base_datos=entorno.get("CATALOGO_BD_URL") or URL_BD_LOCAL_POR_DEFECTO,
        )
        if not configuracion.url_base_datos.startswith("postgresql+psycopg://"):
            raise ConfiguracionInvalidaError(
                "CATALOGO_BD_URL debe empezar por 'postgresql+psycopg://' (CONTRATOS.md 9.2)."
            )
        return configuracion


def _leer_puerto(entorno: Mapping[str, str], nombre: str, por_defecto: int) -> int:
    """Lee un puerto de red de una variable de entorno.

    Args:
        entorno: Variables de entorno.
        nombre: Nombre de la variable.
        por_defecto: Valor si la variable no existe o está vacía.

    Returns:
        El puerto.

    Raises:
        ConfiguracionInvalidaError: Si el valor no es un entero entre 1 y 65535.
    """
    texto = entorno.get(nombre)
    if not texto:
        return por_defecto
    try:
        puerto = int(texto)
    except ValueError:
        puerto = 0
    if not 1 <= puerto <= 65535:
        raise ConfiguracionInvalidaError(f"{nombre}='{texto}' no es un puerto válido (1-65535).")
    return puerto
