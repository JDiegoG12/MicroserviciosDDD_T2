"""Errores de la API: ``application/problem+json`` (CONTRATOS.md 5.2) y mapa de códigos HTTP (5.3).

Un manejador global traduce **cada** excepción con ``codigo`` del dominio o la aplicación al
HTTP de la tabla 5.3. Nunca se exponen trazas: lo inesperado es 500 ``ERROR_INTERNO``.
"""

from __future__ import annotations

import logging
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from catalogo.dominio.excepciones import CatalogoExcepcion
from catalogo.infraestructura.correlacion import obtener_id_correlacion

registro = logging.getLogger(__name__)

TIPO_MEDIO_PROBLEMA = "application/problem+json"
URL_BASE_TIPO_ERROR = "https://banco-preguntas/errores/"

ESTADO_Y_TITULO_POR_CODIGO: dict[str, tuple[int, str]] = {
    "SOLICITUD_INVALIDA": (400, "Solicitud inválida"),
    "NO_AUTENTICADO": (401, "No autenticado"),
    "ACCESO_DENEGADO": (403, "Acceso denegado"),
    "COMPETENCIA_NO_ENCONTRADA": (404, "Competencia no encontrada"),
    "TEMA_NO_ENCONTRADO": (404, "Tema no encontrado"),
    "SUBTEMA_NO_ENCONTRADO": (404, "Subtema no encontrado"),
    "NOMBRE_DUPLICADO": (409, "Nombre duplicado"),
    "BASE_DE_DATOS_NO_DISPONIBLE": (503, "Base de datos no disponible"),
    "ERROR_INTERNO": (500, "Error interno"),
    # CONTRATOS 5.3 (v1.10): errores de protocolo comunes a los tres servicios.
    "RECURSO_NO_ENCONTRADO": (404, "Recurso no encontrado"),
    "METODO_NO_PERMITIDO": (405, "Método no permitido"),
    "TIPO_DE_CONTENIDO_NO_SOPORTADO": (415, "Tipo de contenido no soportado"),
}
"""Código de error → (estado HTTP, título). CONTRATOS.md 5.3 y 8.2."""

TRADUCCIONES_PYDANTIC: dict[str, str] = {
    "missing": "El campo es obligatorio.",
    "string_type": "Debe ser un texto.",
    "string_too_short": "Debe tener al menos {min_length} caracteres.",
    "string_too_long": "No puede superar {max_length} caracteres.",
    "json_invalid": "El cuerpo no es un JSON válido.",
    "model_attributes_type": "El cuerpo debe ser un objeto JSON.",
}
"""Mensajes en español de los errores de validación más comunes de Pydantic."""


def crear_problema(
    solicitud_ruta: str,
    codigo: str,
    detalle: str,
    errores: list[dict[str, str]] | None = None,
) -> JSONResponse:
    """Construye la respuesta de error ``application/problem+json``.

    Args:
        solicitud_ruta: Ruta de la petición (campo ``instance``).
        codigo: Código de error de ``ESTADO_Y_TITULO_POR_CODIGO``.
        detalle: Texto legible en español.
        errores: Campos inválidos (``campo`` y ``mensaje``), si aplica.

    Returns:
        La respuesta, con el estado HTTP que corresponde al código y la correlación en el cuerpo.
    """
    estado, titulo = ESTADO_Y_TITULO_POR_CODIGO[codigo]
    cuerpo: dict[str, Any] = {
        "type": URL_BASE_TIPO_ERROR + codigo,
        "title": titulo,
        "status": estado,
        "detail": detalle,
        "instance": solicitud_ruta,
        "codigo": codigo,
        "idCorrelacion": obtener_id_correlacion(),
        "errores": errores or [],
    }
    return JSONResponse(cuerpo, status_code=estado, media_type=TIPO_MEDIO_PROBLEMA)


def _traducir_errores_de_validacion(error: RequestValidationError) -> list[dict[str, str]]:
    """Convierte los errores de Pydantic en la lista ``errores`` de 5.2.

    Args:
        error: Error de validación de FastAPI.

    Returns:
        Una entrada por campo inválido, con el nombre del campo tal como lo ve el cliente.
    """
    resultado = []
    for detalle in error.errors():
        contexto = detalle.get("ctx") or {}
        if detalle["type"] == "json_invalid":
            # La ubicación de un JSON ilegible es la posición del carácter, no un campo.
            campo = "cuerpo"
        else:
            partes = [str(p) for p in detalle["loc"] if p not in ("body", "query", "path")]
            campo = ".".join(partes) or "cuerpo"
        if detalle["type"] == "string_too_short" and contexto.get("min_length") == 1:
            mensaje = "No puede estar vacío."
        else:
            plantilla = TRADUCCIONES_PYDANTIC.get(detalle["type"], "El valor no es válido.")
            try:
                mensaje = plantilla.format(**contexto)
            except (KeyError, IndexError):
                mensaje = plantilla
        resultado.append({"campo": campo, "mensaje": mensaje})
    return resultado


def instalar_manejadores_de_errores(aplicacion: FastAPI) -> None:
    """Registra los manejadores globales de errores en la aplicación.

    Args:
        aplicacion: Aplicación FastAPI.
    """

    @aplicacion.exception_handler(CatalogoExcepcion)
    async def manejar_excepcion_de_catalogo(solicitud: Request, error: CatalogoExcepcion):
        """Traduce cualquier excepción con ``codigo`` al HTTP de la tabla 5.3."""
        codigo = error.codigo if error.codigo in ESTADO_Y_TITULO_POR_CODIGO else "ERROR_INTERNO"
        if codigo == "ERROR_INTERNO":
            registro.error("Excepción sin traducción HTTP: %s", type(error).__name__)
            return crear_problema(solicitud.url.path, codigo, "Ocurrió un error inesperado.")
        nivel = logging.ERROR if codigo == "BASE_DE_DATOS_NO_DISPONIBLE" else logging.INFO
        registro.log(nivel, "Respuesta de error %s: %s", codigo, error.mensaje)
        return crear_problema(solicitud.url.path, codigo, error.mensaje)

    @aplicacion.exception_handler(RequestValidationError)
    async def manejar_validacion(solicitud: Request, error: RequestValidationError):
        """Convierte la validación de forma de FastAPI (422 por defecto) en 400 ``SOLICITUD_INVALIDA``."""
        # CONTRATOS 5.3: la forma inválida del cuerpo es 400, no el 422 por defecto de FastAPI.
        errores = _traducir_errores_de_validacion(error)
        registro.info("Solicitud inválida: %s", errores)
        return crear_problema(
            solicitud.url.path,
            "SOLICITUD_INVALIDA",
            "La solicitud tiene campos inválidos o faltantes.",
            errores,
        )

    @aplicacion.exception_handler(StarletteHTTPException)
    async def manejar_http(solicitud: Request, error: StarletteHTTPException):
        """Da formato ``problem+json`` a los errores HTTP del framework (404, 405, cuerpo ilegible)."""
        if error.status_code == 404:
            return crear_problema(
                solicitud.url.path, "RECURSO_NO_ENCONTRADO", "La ruta solicitada no existe."
            )
        if error.status_code == 405:
            return crear_problema(
                solicitud.url.path, "METODO_NO_PERMITIDO", "El método HTTP no está permitido en esta ruta."
            )
        if error.status_code == 400:
            return crear_problema(
                solicitud.url.path,
                "SOLICITUD_INVALIDA",
                "No se pudo leer el cuerpo de la solicitud: debe ser un JSON válido en UTF-8.",
            )
        codigo = "SOLICITUD_INVALIDA" if error.status_code < 500 else "ERROR_INTERNO"
        return crear_problema(solicitud.url.path, codigo, "No se pudo atender la solicitud.")
