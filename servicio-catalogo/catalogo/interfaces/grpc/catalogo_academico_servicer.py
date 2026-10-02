"""*Servicer* gRPC de ``CatalogoAcademico`` (CONTRATOS.md sección 6).

Solo traduce: proto → ``ValidarClasificacionCasoUso`` → proto. No tiene reglas de negocio.
"""

from __future__ import annotations

import logging
from typing import Any

import grpc

from catalogo.aplicacion.casos_uso.validar_clasificacion_caso_uso import (
    ValidarClasificacionCasoUso,
)
from catalogo.aplicacion.dtos.comandos import ValidarClasificacionConsulta
from catalogo.aplicacion.excepciones import BaseDeDatosNoDisponibleExcepcion
from catalogo.aplicacion.puertos.salida.ejecutor_transaccional_puerto import (
    EjecutorTransaccionalPuerto,
)
from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion
from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion
from catalogo.infraestructura.correlacion import (
    METADATO_CORRELACION,
    correlacion_activa,
    normalizar_id_correlacion,
)
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2 as pb2
from catalogo.interfaces.grpc.generado import catalogo_academico_pb2_grpc as pb2_grpc

registro = logging.getLogger(__name__)

PREFIJO_MOTIVO_PROTO = "MOTIVO_RECHAZO_"
"""Los valores del enum del proto son los del enum de dominio con este prefijo."""


def motivo_a_proto(motivo: MotivoRechazoClasificacion) -> int:
    """Convierte el enum de dominio en el enum ``MotivoRechazo`` del proto, por nombre.

    Args:
        motivo: Motivo de dominio.

    Returns:
        El valor numérico de ``MotivoRechazo`` con el mismo nombre (más el prefijo).
    """
    return pb2.MotivoRechazo.Value(PREFIJO_MOTIVO_PROTO + motivo.name)


def leer_correlacion(contexto: Any) -> str:
    """Lee el metadato ``x-id-correlacion`` de la llamada o genera uno.

    Args:
        contexto: Contexto de la llamada gRPC.

    Returns:
        Un UUID en texto.
    """
    metadatos = dict(contexto.invocation_metadata() or ())
    return normalizar_id_correlacion(metadatos.get(METADATO_CORRELACION))


class CatalogoAcademicoServicer(pb2_grpc.CatalogoAcademicoServicer):
    """Implementa el servicio ``CatalogoAcademico`` (*Open Host Service*).

    Una clasificación inválida **no** es un error gRPC: se responde ``OK`` con
    ``valida=false`` y el motivo. Un id que no es UUID → ``INVALID_ARGUMENT``. Si la base
    de datos no responde → ``UNAVAILABLE``.
    """

    def __init__(self, ejecutor: EjecutorTransaccionalPuerto) -> None:
        """Crea el servicer.

        Args:
            ejecutor: Ejecuta el caso de uso dentro de una transacción.
        """
        self._ejecutor = ejecutor

    async def ValidarClasificacion(  # noqa: N802 (nombre fijado por el .proto)
        self, request: pb2.ValidarClasificacionSolicitud, context: Any
    ) -> pb2.ValidarClasificacionRespuesta:
        """Valida la terna Competencia/Tema/Subtema (INV-07, D-13).

        Args:
            request: Terna de identificadores en texto.
            context: Contexto de la llamada gRPC.

        Returns:
            El resultado de la validación, con el primer motivo de rechazo si no es válida.
        """
        with correlacion_activa(leer_correlacion(context)):
            consulta = ValidarClasificacionConsulta(
                request.competencia_id, request.tema_id, request.subtema_id
            )
            registro.info(
                "ValidarClasificacion competencia=%s tema=%s subtema=%s",
                consulta.competencia_id,
                consulta.tema_id,
                consulta.subtema_id,
            )
            try:
                respuesta = await self._ejecutor.ejecutar(
                    lambda repositorio, _publicador: ValidarClasificacionCasoUso(
                        repositorio
                    ).ejecutar(consulta)
                )
            except SolicitudInvalidaExcepcion as error:
                registro.warning("ValidarClasificacion rechazada: %s", error.mensaje)
                await context.abort(grpc.StatusCode.INVALID_ARGUMENT, error.mensaje)
            except BaseDeDatosNoDisponibleExcepcion as error:
                # CONTRATOS 6 (v1.11): base de datos caída o esquema aún no listo -> UNAVAILABLE
                # con mensaje en español; Editorial lo traduce a 503 CATALOGO_NO_DISPONIBLE.
                registro.error("ValidarClasificacion sin base de datos: %s", error.mensaje)
                await context.abort(grpc.StatusCode.UNAVAILABLE, error.mensaje)
            except Exception:
                # CONTRATOS 6 (v1.11): error inesperado -> INTERNAL sin detalles internos.
                registro.exception("ValidarClasificacion falló con un error inesperado")
                await context.abort(grpc.StatusCode.INTERNAL, "Error interno del catálogo.")
            registro.info(
                "ValidarClasificacion resultado valida=%s motivo=%s",
                respuesta.valida,
                respuesta.motivo.name,
            )
            return pb2.ValidarClasificacionRespuesta(
                valida=respuesta.valida,
                motivo=motivo_a_proto(respuesta.motivo),
                detalle=respuesta.detalle,
            )
