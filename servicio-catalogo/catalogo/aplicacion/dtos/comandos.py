"""Comandos y consultas de entrada de los casos de uso.

Los identificadores llegan como texto (UUID); el caso de uso los valida y, si están mal
formados, lanza ``SOLICITUD_INVALIDA`` (CONTRATOS.md 5.3).
"""

from __future__ import annotations

from dataclasses import dataclass

from catalogo.dominio.sin_cambio import SIN_CAMBIO, SinCambio


@dataclass(frozen=True, slots=True)
class CrearCompetenciaComando:
    """Datos para crear una competencia (``POST /competencias``).

    Attributes:
        nombre: Nombre de 1 a 120 caracteres.
        descripcion: Descripción opcional de hasta 500 caracteres.
    """

    nombre: str
    descripcion: str | None = None


@dataclass(frozen=True, slots=True)
class RenombrarCompetenciaComando:
    """Datos para renombrar una competencia (``PUT /competencias/{competenciaId}``).

    Attributes:
        competencia_id: Competencia a renombrar.
        nombre: Nombre nuevo.
        descripcion: ``SIN_CAMBIO`` (no vino en el cuerpo) conserva la actual; ``None`` o
            ``""`` la borra; un texto la reemplaza (CONTRATOS.md 8.2, v1.9).
    """

    competencia_id: str
    nombre: str
    descripcion: str | None | SinCambio = SIN_CAMBIO


@dataclass(frozen=True, slots=True)
class AgregarTemaComando:
    """Datos para agregar un tema (``POST /competencias/{competenciaId}/temas``).

    Attributes:
        competencia_id: Competencia dueña del tema.
        nombre: Nombre del tema.
    """

    competencia_id: str
    nombre: str


@dataclass(frozen=True, slots=True)
class RenombrarTemaComando:
    """Datos para renombrar un tema (``PUT /competencias/{competenciaId}/temas/{temaId}``).

    Attributes:
        competencia_id: Competencia dueña del tema.
        tema_id: Tema a renombrar.
        nombre: Nombre nuevo.
    """

    competencia_id: str
    tema_id: str
    nombre: str


@dataclass(frozen=True, slots=True)
class AgregarSubtemaComando:
    """Datos para agregar un subtema (``POST .../temas/{temaId}/subtemas``).

    Attributes:
        competencia_id: Competencia dueña del tema.
        tema_id: Tema al que se agrega el subtema.
        nombre: Nombre del subtema.
    """

    competencia_id: str
    tema_id: str
    nombre: str


@dataclass(frozen=True, slots=True)
class RenombrarSubtemaComando:
    """Datos para renombrar un subtema (``PUT .../temas/{temaId}/subtemas/{subtemaId}``).

    Attributes:
        competencia_id: Competencia dueña del tema.
        tema_id: Tema al que pertenece el subtema.
        subtema_id: Subtema a renombrar.
        nombre: Nombre nuevo.
    """

    competencia_id: str
    tema_id: str
    subtema_id: str
    nombre: str


@dataclass(frozen=True, slots=True)
class ObtenerCompetenciaConsulta:
    """Datos para consultar una competencia (``GET /competencias/{competenciaId}``).

    Attributes:
        competencia_id: Competencia buscada.
    """

    competencia_id: str


@dataclass(frozen=True, slots=True)
class ValidarClasificacionConsulta:
    """Terna a validar (gRPC ``ValidarClasificacion``).

    Attributes:
        competencia_id: UUID de la competencia, en texto.
        tema_id: UUID del tema, en texto.
        subtema_id: UUID del subtema, en texto.
    """

    competencia_id: str
    tema_id: str
    subtema_id: str
