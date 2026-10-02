"""Creación del servidor ``grpc.aio`` con reflexión activada (CONTRATOS.md 6)."""

from __future__ import annotations

import grpc
from grpc_reflection.v1alpha import reflection

from catalogo.aplicacion.puertos.salida.ejecutor_transaccional_puerto import (
    EjecutorTransaccionalPuerto,
)
from catalogo.interfaces.grpc.catalogo_academico_servicer import CatalogoAcademicoServicer
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2 as pb2
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2_grpc as pb2_grpc

NOMBRE_SERVICIO_GRPC = pb2.DESCRIPTOR.services_by_name["CatalogoAcademico"].full_name
"""Nombre completo: ``bancopreguntas.catalogo.v1.CatalogoAcademico``."""


def crear_servidor_grpc(ejecutor: EjecutorTransaccionalPuerto) -> grpc.aio.Server:
    """Crea el servidor gRPC con el servicio ``CatalogoAcademico`` y la reflexión.

    El servidor se crea sin puerto: quien lo usa llama a ``add_insecure_port`` y ``start``.
    Sin TLS: es una red interna (CONTRATOS.md 6).

    Args:
        ejecutor: Ejecuta los casos de uso dentro de una transacción.

    Returns:
        El servidor, todavía sin arrancar.
    """
    # so_reuseport=0: con un puerto ocupado debe fallar al arrancar (CONTRATOS 11.2), no
    # compartirlo en silencio con otro proceso.
    servidor = grpc.aio.server(options=(("grpc.so_reuseport", 0),))
    pb2_grpc.add_CatalogoAcademicoServicer_to_server(CatalogoAcademicoServicer(ejecutor), servidor)
    reflection.enable_server_reflection((NOMBRE_SERVICIO_GRPC, reflection.SERVICE_NAME), servidor)
    return servidor
