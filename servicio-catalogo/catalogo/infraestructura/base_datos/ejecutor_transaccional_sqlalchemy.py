"""Implementación de ``EjecutorTransaccionalPuerto`` con SQLAlchemy asíncrono."""

from __future__ import annotations

import logging
from collections.abc import Callable
from dataclasses import dataclass

from sqlalchemy.exc import IntegrityError, InterfaceError, OperationalError
from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine

from catalogo.aplicacion.excepciones import BaseDeDatosNoDisponibleExcepcion
from catalogo.aplicacion.puertos.salida.ejecutor_transaccional_puerto import (
    EjecutorTransaccionalPuerto,
)
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.excepciones import NombreDuplicadoExcepcion
from catalogo.infraestructura.base_datos.competencia_repositorio_sqlalchemy import (
    CompetenciaRepositorioSqlAlchemy,
)
from catalogo.infraestructura.base_datos.modelos import RESTRICCIONES_DE_NOMBRE

registro = logging.getLogger(__name__)

SEGUNDOS_ESPERA_CONEXION = 5
"""Tiempo máximo para conectar: sin esto, una base caída colgaría cada petición."""


@dataclass(slots=True)
class EstadoInicializacion:
    """Indica si la base de datos ya tiene el esquema (migraciones aplicadas) y la siembra.

    Mientras ``catalogo_listo`` sea falso, las operaciones responden
    ``BASE_DE_DATOS_NO_DISPONIBLE`` (CONTRATOS.md 9.3.6, v1.10) en vez de fallar con un error
    de tabla inexistente. REST lo traduce a 503 y gRPC a ``UNAVAILABLE``.

    Attributes:
        catalogo_listo: ``True`` cuando las migraciones y la siembra terminaron.
    """

    catalogo_listo: bool = False


def crear_motor(url_base_datos: str) -> AsyncEngine:
    """Crea el motor asíncrono de SQLAlchemy con ``psycopg`` 3.

    El motor no abre conexiones hasta usarse, así que el servicio arranca aunque la base de
    datos todavía no esté lista (CONTRATOS.md 9.3.6).

    Args:
        url_base_datos: URL ``postgresql+psycopg://usuario:clave@host:puerto/base``.

    Returns:
        El motor.
    """
    return create_async_engine(
        url_base_datos,
        pool_pre_ping=True,
        connect_args={"connect_timeout": SEGUNDOS_ESPERA_CONEXION},
    )


class EjecutorTransaccionalSqlAlchemy(EjecutorTransaccionalPuerto):
    """Ejecuta cada caso de uso en una transacción de ``AsyncSession``.

    Los casos de uso son síncronos: se ejecutan con ``AsyncSession.run_sync``, que les
    permite usar una ``Session`` de estilo síncrono sobre el motor asíncrono sin bloquear
    el bucle de eventos. Si el caso de uso termina bien se hace ``COMMIT``; si lanza una
    excepción, ``ROLLBACK``. Cada llamada es una transacción (la siembra completa también).
    """

    def __init__(
        self,
        fabrica_sesiones: async_sessionmaker[AsyncSession],
        publicador: PublicadorEventosPuerto,
        estado: EstadoInicializacion,
    ) -> None:
        """Crea el ejecutor.

        Args:
            fabrica_sesiones: Fábrica de sesiones asíncronas.
            publicador: Puerto que se entrega a los casos de uso.
            estado: Estado de la inicialización de la base de datos.
        """
        self._fabrica_sesiones = fabrica_sesiones
        self._publicador = publicador
        self._estado = estado

    async def ejecutar[R](
        self,
        operacion: Callable[[CompetenciaRepositorio, PublicadorEventosPuerto], R],
        *,
        requiere_catalogo_listo: bool = True,
    ) -> R:
        """Ejecuta la operación en una transacción.

        Args:
            operacion: Función que construye el caso de uso y lo ejecuta.
            requiere_catalogo_listo: Si es ``True`` y las migraciones aún no terminaron, responde
                ``BASE_DE_DATOS_NO_DISPONIBLE``. La inicialización lo pone en ``False``.

        Returns:
            Lo que devuelva la operación.

        Raises:
            BaseDeDatosNoDisponibleExcepcion: Si la base no responde o aún no está lista.
            NombreDuplicadoExcepcion: Si un índice único de nombre rechaza la escritura
                (dos peticiones concurrentes con el mismo nombre; INV-24).
        """
        if requiere_catalogo_listo and not self._estado.catalogo_listo:
            raise BaseDeDatosNoDisponibleExcepcion(
                "La base de datos del catálogo aún se está inicializando."
            )
        try:
            async with self._fabrica_sesiones() as sesion, sesion.begin():
                return await sesion.run_sync(
                    lambda sesion_sincrona: operacion(
                        CompetenciaRepositorioSqlAlchemy(sesion_sincrona), self._publicador
                    )
                )
        except IntegrityError as error:
            nombre_restriccion = _nombre_de_restriccion(error)
            if nombre_restriccion in RESTRICCIONES_DE_NOMBRE:
                raise NombreDuplicadoExcepcion(
                    "Ya existe un elemento del catálogo con ese nombre en su ámbito."
                ) from error
            raise
        except (OperationalError, InterfaceError, OSError) as error:
            registro.error("La base de datos no responde: %s", error.__class__.__name__)
            raise BaseDeDatosNoDisponibleExcepcion(
                "La base de datos del catálogo no responde."
            ) from error


def _nombre_de_restriccion(error: IntegrityError) -> str | None:
    """Obtiene el nombre de la restricción violada, si PostgreSQL lo informa.

    Args:
        error: Error de integridad de SQLAlchemy.

    Returns:
        El nombre de la restricción, o ``None`` si no se puede determinar.
    """
    diagnostico = getattr(error.orig, "diag", None)
    return getattr(diagnostico, "constraint_name", None)
