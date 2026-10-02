"""Configuración del log: una línea por evento, en UTC y con ``idCorrelacion`` siempre."""

from __future__ import annotations

import logging
import sys
import time

from catalogo.infraestructura.correlacion import obtener_id_correlacion

NOMBRE_SERVICIO = "servicio-catalogo"
FORMATO_LINEA = (
    "%(asctime)s %(levelname)s " + NOMBRE_SERVICIO + " idCorrelacion=%(idCorrelacion)s "
    "%(name)s - %(message)s"
)


class FiltroCorrelacion(logging.Filter):
    """Agrega ``idCorrelacion`` a todos los registros que pasan por el manejador."""

    def filter(self, record: logging.LogRecord) -> bool:
        """Anota en el registro la correlación activa.

        Args:
            record: Registro de log.

        Returns:
            Siempre ``True``: el filtro solo enriquece, no descarta.
        """
        record.idCorrelacion = obtener_id_correlacion()
        return True


def configurar_registro_log(nivel: str = "INFO") -> None:
    """Deja el log raíz escribiendo en la salida estándar con el formato del servicio.

    Las líneas salen en UTC. Los registros de uvicorn y de gRPC pasan por el mismo
    manejador, así que también llevan ``idCorrelacion``.

    Args:
        nivel: Nivel mínimo (``INFO``, ``DEBUG``...).
    """
    manejador = logging.StreamHandler(sys.stdout)
    formateador = logging.Formatter(FORMATO_LINEA, datefmt="%Y-%m-%dT%H:%M:%S")
    formateador.converter = time.gmtime
    manejador.setFormatter(formateador)
    manejador.addFilter(FiltroCorrelacion())
    raiz = logging.getLogger()
    raiz.handlers.clear()
    raiz.addHandler(manejador)
    raiz.setLevel(nivel)
    for nombre in ("uvicorn", "uvicorn.error", "uvicorn.access"):
        registro = logging.getLogger(nombre)
        registro.handlers.clear()
        registro.propagate = True
