"""DTOs JSON de la API REST (propios de ``interfaces``), en camelCase (CONTRATOS.md 4 y 8.2).

No exponen entidades de dominio ni modelos de base de datos. Las claves usan
``alias_generator=to_camel`` y ``populate_by_name=True``; las respuestas salen
``by_alias`` (comportamiento por defecto de FastAPI).
"""

from __future__ import annotations

from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, StringConstraints
from pydantic.alias_generators import to_camel

from catalogo.dominio.competencia import LONGITUD_MAXIMA_DESCRIPCION
from catalogo.dominio.nombre_catalogo import LONGITUD_MAXIMA_NOMBRE


class ModeloCamel(BaseModel):
    """Base de los DTOs: claves camelCase, y se pueden construir desde atributos."""

    model_config = ConfigDict(
        alias_generator=to_camel, populate_by_name=True, from_attributes=True
    )


# El formato del campo (longitudes, después de quitar los espacios de los extremos) se valida
# aquí con 400 SOLICITUD_INVALIDA (CONTRATOS.md 5.3). El dominio repite estas reglas como
# defensa propia: INV-24 y la longitud de los nombres no dependen de la API.
NombreTexto = Annotated[
    str,
    StringConstraints(strip_whitespace=True, min_length=1, max_length=LONGITUD_MAXIMA_NOMBRE),
]
DescripcionTexto = Annotated[
    str, StringConstraints(strip_whitespace=True, max_length=LONGITUD_MAXIMA_DESCRIPCION)
]


class CompetenciaSolicitud(ModeloCamel):
    """Cuerpo de ``POST /competencias`` y ``PUT /competencias/{competenciaId}``."""

    nombre: NombreTexto = Field(
        description="Nombre de la competencia: de 1 a 120 caracteres. Debe ser único en el "
        "catálogo sin distinguir mayúsculas ni tildes (la ñ cuenta como letra propia).",
        examples=["Comunicación escrita"],
    )
    descripcion: DescripcionTexto | None = Field(
        default=None,
        description="Descripción opcional de hasta 500 caracteres. En el PUT: si no viene se "
        "conserva la actual; si viene como `null` o `\"\"` se borra; si viene con texto se "
        "reemplaza.",
        examples=["Lectura crítica y producción de textos"],
    )

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True,
        json_schema_extra={
            "examples": [
                {"nombre": "Comunicación escrita", "descripcion": "Lectura crítica y producción de textos"}
            ]
        },
    )


class NombreSolicitud(ModeloCamel):
    """Cuerpo de las operaciones de temas y subtemas (no tienen descripción, CONTRATOS.md 8.2)."""

    nombre: NombreTexto = Field(
        description="Nombre de 1 a 120 caracteres, único dentro de su competencia (tema) o de "
        "su tema (subtema), sin distinguir mayúsculas ni tildes.",
        examples=["Geometría"],
    )

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True,
        json_schema_extra={"examples": [{"nombre": "Geometría"}]},
    )


class SubtemaRespuestaModelo(ModeloCamel):
    """Subtema dentro de una competencia."""

    subtema_id: str = Field(examples=["22222222-2222-4222-8222-000000000301"])
    nombre: str = Field(examples=["Medidas de tendencia central"])


class TemaRespuestaModelo(ModeloCamel):
    """Tema con sus subtemas."""

    tema_id: str = Field(examples=["22222222-2222-4222-8222-000000000201"])
    nombre: str = Field(examples=["Estadística"])
    subtemas: list[SubtemaRespuestaModelo]


class CompetenciaRespuestaModelo(ModeloCamel):
    """``CompetenciaRespuesta`` de CONTRATOS.md 8.2: competencia con temas y subtemas anidados."""

    competencia_id: str = Field(examples=["22222222-2222-4222-8222-000000000101"])
    nombre: str = Field(examples=["Razonamiento cuantitativo"])
    descripcion: str | None = Field(default=None)
    temas: list[TemaRespuestaModelo]

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True,
        json_schema_extra={
            "examples": [
                {
                    "competenciaId": "22222222-2222-4222-8222-000000000101",
                    "nombre": "Razonamiento cuantitativo",
                    "descripcion": None,
                    "temas": [
                        {
                            "temaId": "22222222-2222-4222-8222-000000000201",
                            "nombre": "Estadística",
                            "subtemas": [
                                {
                                    "subtemaId": "22222222-2222-4222-8222-000000000301",
                                    "nombre": "Medidas de tendencia central",
                                }
                            ],
                        }
                    ],
                }
            ]
        },
    )


class ErrorDeCampo(ModeloCamel):
    """Un campo inválido dentro de ``errores`` (CONTRATOS.md 5.2)."""

    campo: str = Field(examples=["nombre"])
    mensaje: str = Field(examples=["No puede superar 120 caracteres."])


class ProblemaRespuesta(ModeloCamel):
    """Cuerpo de error ``application/problem+json`` (RFC 7807, CONTRATOS.md 5.2)."""

    type: str = Field(examples=["https://banco-preguntas/errores/NOMBRE_DUPLICADO"])
    title: str = Field(examples=["Nombre duplicado"])
    status: int = Field(examples=[409])
    detail: str = Field(examples=["Ya existe una competencia llamada 'Diseño de software'."])
    instance: str = Field(examples=["/api/v1/competencias"])
    codigo: str = Field(examples=["NOMBRE_DUPLICADO"])
    id_correlacion: str = Field(examples=["9a1b6f2c-1d3a-4e5f-8a7b-9c0d1e2f3a4b"])
    errores: list[ErrorDeCampo] = Field(
        default_factory=list, description="Campos inválidos (solo en errores de validación)."
    )


class EstadoSalud(ModeloCamel):
    """Respuesta de ``GET /salud``."""

    estado: str = Field(examples=["OK"])
    servicio: str = Field(examples=["servicio-catalogo"])
