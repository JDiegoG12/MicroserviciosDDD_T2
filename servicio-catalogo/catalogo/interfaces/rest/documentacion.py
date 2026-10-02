"""Ayudas para documentar en OpenAPI las respuestas de error de cada endpoint."""

from __future__ import annotations

from typing import Any

from catalogo.interfaces.rest.errores import (
    ESTADO_Y_TITULO_POR_CODIGO,
    TIPO_MEDIO_PROBLEMA,
    URL_BASE_TIPO_ERROR,
)
from catalogo.interfaces.rest.esquemas import ProblemaRespuesta

DETALLES_DE_EJEMPLO = {
    "SOLICITUD_INVALIDA": "La solicitud tiene campos inválidos o faltantes.",
    "NO_AUTENTICADO": "Faltan los encabezados X-Usuario-Id y X-Roles.",
    "ACCESO_DENEGADO": "Solo un ADMINISTRADOR puede modificar el catálogo.",
    "COMPETENCIA_NO_ENCONTRADA": "La competencia 22222222-2222-4222-8222-000000000999 no existe.",
    "TEMA_NO_ENCONTRADO": "El tema 22222222-2222-4222-8222-000000000999 no existe en la competencia.",
    "SUBTEMA_NO_ENCONTRADO": "El subtema 22222222-2222-4222-8222-000000000999 no existe en el tema.",
    "NOMBRE_DUPLICADO": "Ya existe una competencia llamada 'Diseño de software' en el catálogo.",
    "BASE_DE_DATOS_NO_DISPONIBLE": "La base de datos del catálogo no responde.",
    "ERROR_INTERNO": "Ocurrió un error inesperado.",
}


def respuestas_de_error(*codigos: str) -> dict[int | str, dict[str, Any]]:
    """Arma el parámetro ``responses`` de un endpoint con todos sus errores.

    Agrupa por estado HTTP (varios códigos pueden compartir estado, como los 404) y agrega
    un ejemplo ``application/problem+json`` por código.

    Args:
        *codigos: Códigos de error que puede devolver el endpoint.

    Returns:
        Diccionario estado → descripción, modelo y ejemplos, para ``responses=``.
    """
    por_estado: dict[int, dict[str, Any]] = {}
    for codigo in codigos:
        estado, titulo = ESTADO_Y_TITULO_POR_CODIGO[codigo]
        entrada = por_estado.setdefault(
            estado, {"model": ProblemaRespuesta, "description": titulo, "content": {}}
        )
        if entrada["description"] != titulo:
            entrada["description"] += f" / {titulo}"
        ejemplos = entrada["content"].setdefault("application/json", {"examples": {}})["examples"]
        ejemplos[codigo] = {
            "summary": titulo,
            "value": {
                "type": URL_BASE_TIPO_ERROR + codigo,
                "title": titulo,
                "status": estado,
                "detail": DETALLES_DE_EJEMPLO.get(codigo, titulo),
                "instance": "/api/v1/competencias",
                "codigo": codigo,
                "idCorrelacion": "9a1b6f2c-1d3a-4e5f-8a7b-9c0d1e2f3a4b",
                "errores": (
                    [{"campo": "nombre", "mensaje": "No puede superar 120 caracteres."}]
                    if codigo == "SOLICITUD_INVALIDA"
                    else []
                ),
            },
        }
    return dict(por_estado)


def ajustar_openapi(esquema: dict[str, Any]) -> dict[str, Any]:
    """Deja el OpenAPI generado coherente con CONTRATOS.md 5.2 y 5.3.

    * Los errores se documentan como ``application/problem+json``, no ``application/json``.
    * Se quita el 422 ``HTTPValidationError`` que FastAPI agrega por defecto: aquí un cuerpo
      inválido es 400 ``SOLICITUD_INVALIDA``.

    Args:
        esquema: Documento OpenAPI generado por FastAPI.

    Returns:
        El mismo documento, corregido.
    """
    for ruta in esquema.get("paths", {}).values():
        for operacion in ruta.values():
            respuestas = operacion.get("responses", {})
            respuestas.pop("422", None)
            for estado, respuesta in respuestas.items():
                if estado.startswith(("4", "5")) and "application/json" in respuesta.get("content", {}):
                    contenido = respuesta["content"]
                    contenido[TIPO_MEDIO_PROBLEMA] = contenido.pop("application/json")
    for nombre in ("HTTPValidationError", "ValidationError"):
        esquema.get("components", {}).get("schemas", {}).pop(nombre, None)
    return esquema
