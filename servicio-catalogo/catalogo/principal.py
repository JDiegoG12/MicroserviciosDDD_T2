"""Punto de entrada del servicio: REST (FastAPI) y gRPC (``grpc.aio``) en un solo proceso.

Ambos servidores corren en el **mismo bucle asyncio** (CONTRATOS.md 11.2). Se ejecuta con::

    python -m catalogo.principal

Al arrancar:

1. se crean los servidores sin tocar la base de datos (el servicio arranca aunque PostgreSQL
   aún no esté lista, CONTRATOS.md 9.3.6);
2. una tarea de fondo aplica las migraciones de Alembic y ejecuta la siembra de la tabla 4.3,
   reintentando con espera progresiva hasta lograrlo. Mientras tanto ``/salud`` responde 200
   y los endpoints que usan la base responden 503;
3. si cualquiera de los dos servidores falla (por ejemplo, un puerto ocupado), el proceso
   termina con un error claro y código distinto de cero, sin quedar a medias.
"""

from __future__ import annotations

import asyncio
import logging
import sys

import uvicorn
from sqlalchemy.engine import make_url
from sqlalchemy.ext.asyncio import async_sessionmaker

from catalogo.infraestructura.base_datos.ejecutor_transaccional_sqlalchemy import (
    EjecutorTransaccionalSqlAlchemy,
    EstadoInicializacion,
    crear_motor,
)
from catalogo.infraestructura.configuracion import Configuracion, ConfiguracionInvalidaError
from catalogo.infraestructura.inicializacion import inicializar_base_de_datos
from catalogo.infraestructura.publicador_eventos_log_adaptador import PublicadorEventosLogAdaptador
from catalogo.infraestructura.registro_log import configurar_registro_log
from catalogo.interfaces.grpc.servidor_grpc import crear_servidor_grpc
from catalogo.interfaces.rest.aplicacion_fastapi import crear_aplicacion

registro = logging.getLogger(__name__)

SEGUNDOS_DE_GRACIA_GRPC = 5


class ErrorDeArranqueError(Exception):
    """Un servidor no pudo arrancar o se detuvo de forma inesperada."""


async def _servir_http(servidor: uvicorn.Server) -> None:
    """Sirve REST hasta que termine; convierte la salida de uvicorn por error en excepción.

    Args:
        servidor: Servidor uvicorn.

    Raises:
        ErrorDeArranqueError: Si uvicorn no puede arrancar (por ejemplo, puerto ocupado).
    """
    try:
        await servidor.serve()
    except SystemExit as salida:  # uvicorn llama a sys.exit(1) si no puede abrir el puerto
        raise ErrorDeArranqueError(
            f"El servidor REST no pudo arrancar (código {salida.code}). "
            "Revise que el puerto HTTP no esté ocupado."
        ) from None


async def servir(configuracion: Configuracion) -> int:
    """Arranca los dos servidores y la inicialización de la base de datos.

    Args:
        configuracion: Puertos y URL de la base de datos.

    Returns:
        ``0`` si terminó por una señal de parada; ``1`` si un servidor falló.
    """
    url_visible = make_url(configuracion.url_base_datos).render_as_string(hide_password=True)
    registro.info(
        "Arrancando servicio-catalogo: REST %s, gRPC %s, base de datos %s",
        configuracion.puerto_http,
        configuracion.puerto_grpc,
        url_visible,
    )
    motor = crear_motor(configuracion.url_base_datos)
    estado = EstadoInicializacion()
    ejecutor = EjecutorTransaccionalSqlAlchemy(
        async_sessionmaker(motor, expire_on_commit=False), PublicadorEventosLogAdaptador(), estado
    )

    servidor_grpc = crear_servidor_grpc(ejecutor)
    try:
        # Según la plataforma, grpc devuelve 0 o lanza RuntimeError si el puerto está ocupado.
        puerto_enlazado = servidor_grpc.add_insecure_port(f"0.0.0.0:{configuracion.puerto_grpc}")
    except RuntimeError:
        puerto_enlazado = 0
    if puerto_enlazado == 0:
        registro.critical(
            "No se pudo abrir el puerto gRPC %s: revise que no esté ocupado.",
            configuracion.puerto_grpc,
        )
        await motor.dispose()
        return 1
    servidor_http = uvicorn.Server(
        uvicorn.Config(
            crear_aplicacion(ejecutor),
            host="0.0.0.0",
            port=configuracion.puerto_http,
            log_config=None,  # el log lo configura registro_log (con idCorrelacion)
            access_log=False,  # MiddlewareCorrelacion registra cada petición
        )
    )

    await servidor_grpc.start()
    registro.info("Servidor gRPC escuchando en el puerto %s (reflexión activada).", configuracion.puerto_grpc)
    tarea_http = asyncio.create_task(_servir_http(servidor_http), name="http")
    tarea_grpc = asyncio.create_task(servidor_grpc.wait_for_termination(), name="grpc")
    tarea_bd = asyncio.create_task(
        inicializar_base_de_datos(motor, ejecutor, estado), name="inicializacion-bd"
    )

    codigo = 0
    try:
        terminadas, _ = await asyncio.wait({tarea_http, tarea_grpc}, return_when=asyncio.FIRST_COMPLETED)
        if tarea_http in terminadas and tarea_http.exception() is None:
            registro.info("Parada solicitada: cerrando los servidores.")
        else:
            codigo = 1
            causa = tarea_http.exception() if tarea_http in terminadas else None
            registro.critical(
                "Un servidor se detuvo de forma inesperada: %s",
                causa or "el servidor gRPC terminó",
            )
    finally:
        servidor_http.should_exit = True
        tarea_bd.cancel()
        await servidor_grpc.stop(grace=SEGUNDOS_DE_GRACIA_GRPC)
        await asyncio.gather(tarea_http, tarea_grpc, tarea_bd, return_exceptions=True)
        await motor.dispose()
    return codigo


def main() -> None:
    """Configura el log, lee las variables de entorno y ejecuta el servicio.

    Termina con ``sys.exit``: ``0`` en una parada normal, ``1`` si un servidor falló y ``2`` si la
    configuración es inválida.
    """
    configurar_registro_log("INFO")
    try:
        configuracion = Configuracion.desde_entorno()
    except ConfiguracionInvalidaError as error:
        registro.critical("Configuración inválida: %s", error)
        sys.exit(2)
    # psycopg asíncrono no funciona con el ProactorEventLoop de Windows (solo al ejecutar
    # localmente en Windows; en el contenedor Linux se usa el bucle por defecto).
    fabrica_bucle = asyncio.SelectorEventLoop if sys.platform == "win32" else None
    try:
        sys.exit(asyncio.run(servir(configuracion), loop_factory=fabrica_bucle))
    except KeyboardInterrupt:
        sys.exit(0)


if __name__ == "__main__":
    main()
