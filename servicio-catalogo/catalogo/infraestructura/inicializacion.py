"""Inicialización de la base de datos al arrancar: migraciones de Alembic y siembra (4.3)."""

from __future__ import annotations

import asyncio
import logging
from pathlib import Path

from alembic import command
from alembic.config import Config
from sqlalchemy import Connection
from sqlalchemy.ext.asyncio import AsyncEngine

from catalogo.aplicacion.casos_uso.sembrar_catalogo_caso_uso import SembrarCatalogoCasoUso
from catalogo.aplicacion.datos_semilla import CATALOGO_SEMILLA
from catalogo.aplicacion.dtos.definiciones_semilla import SembrarCatalogoComando
from catalogo.infraestructura.base_datos.ejecutor_transaccional_sqlalchemy import (
    EjecutorTransaccionalSqlAlchemy,
    EstadoInicializacion,
)

registro = logging.getLogger(__name__)

CARPETA_MIGRACIONES = Path(__file__).resolve().parent / "migraciones"
ESPERA_INICIAL_SEGUNDOS = 1.0
ESPERA_MAXIMA_SEGUNDOS = 30.0


def _configuracion_alembic(conexion: Connection) -> Config:
    """Arma la configuración de Alembic para usar una conexión ya abierta.

    Args:
        conexion: Conexión síncrona dentro de ``run_sync``.

    Returns:
        La configuración, con ``script_location`` en la carpeta del paquete.
    """
    configuracion = Config()
    configuracion.set_main_option("script_location", str(CARPETA_MIGRACIONES))
    configuracion.attributes["connection"] = conexion
    return configuracion


async def aplicar_migraciones(motor: AsyncEngine) -> None:
    """Lleva el esquema a la última versión de Alembic (``upgrade head``) en una transacción.

    Args:
        motor: Motor asíncrono de la base de datos del catálogo.
    """
    async with motor.begin() as conexion:
        await conexion.run_sync(
            lambda sincrona: command.upgrade(_configuracion_alembic(sincrona), "head")
        )


async def sembrar_catalogo(ejecutor: EjecutorTransaccionalSqlAlchemy) -> bool:
    """Ejecuta ``SembrarCatalogoCasoUso`` con la tabla 4.3 en una sola transacción.

    Args:
        ejecutor: Ejecutor transaccional.

    Returns:
        ``True`` si sembró; ``False`` si el catálogo ya tenía datos (idempotente).
    """
    resultado = await ejecutor.ejecutar(
        lambda repositorio, publicador: SembrarCatalogoCasoUso(repositorio, publicador).ejecutar(
            SembrarCatalogoComando(CATALOGO_SEMILLA)
        ),
        requiere_catalogo_listo=False,
    )
    return resultado.sembrado


async def inicializar_base_de_datos(
    motor: AsyncEngine,
    ejecutor: EjecutorTransaccionalSqlAlchemy,
    estado: EstadoInicializacion,
) -> None:
    """Aplica las migraciones y siembra el catálogo, reintentando hasta lograrlo.

    El servicio arranca aunque la base de datos todavía no esté lista (CONTRATOS.md 9.3.6,
    v1.10): las migraciones y la siembra van en segundo plano y, mientras tanto, REST responde
    503 y gRPC ``UNAVAILABLE``. Esta tarea reintenta con espera progresiva (1, 2, 4... hasta
    un máximo de 30 s entre intentos), escribe cada intento y su resultado en el log y marca
    ``estado.catalogo_listo`` al terminar. Las migraciones van primero y la siembra después,
    en una sola transacción.

    Args:
        motor: Motor asíncrono.
        ejecutor: Ejecutor transaccional.
        estado: Estado que se marca como listo.
    """
    espera = ESPERA_INICIAL_SEGUNDOS
    intento = 0
    while True:
        intento += 1
        registro.info("Inicialización de la base de datos, intento %d: migraciones y siembra.", intento)
        try:
            await aplicar_migraciones(motor)
            sembrado = await sembrar_catalogo(ejecutor)
        except asyncio.CancelledError:
            raise
        except Exception as error:
            registro.warning(
                "Intento %d fallido: la base de datos aún no está lista (%s). Se reintenta en %.1f s.",
                intento,
                error.__class__.__name__,
                espera,
            )
            await asyncio.sleep(espera)
            espera = min(espera * 2, ESPERA_MAXIMA_SEGUNDOS)
            continue
        estado.catalogo_listo = True
        registro.info(
            "Catálogo listo tras %d intento(s): migraciones aplicadas; %s.",
            intento,
            "catálogo sembrado" if sembrado else "el catálogo ya tenía datos, no se sembró",
        )
        return
