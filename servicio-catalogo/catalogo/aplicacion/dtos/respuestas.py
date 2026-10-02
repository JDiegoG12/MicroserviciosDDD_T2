"""Resultados (DTOs) de los casos de uso, con la forma de ``CompetenciaRespuesta`` (CONTRATOS.md 8.2)."""

from __future__ import annotations

from dataclasses import dataclass

from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion


@dataclass(frozen=True, slots=True)
class SubtemaRespuesta:
    """Subtema dentro de una ``CompetenciaRespuesta``.

    Attributes:
        subtema_id: UUID del subtema, en texto.
        nombre: Nombre del subtema.
    """

    subtema_id: str
    nombre: str


@dataclass(frozen=True, slots=True)
class TemaRespuesta:
    """Tema dentro de una ``CompetenciaRespuesta``.

    Attributes:
        tema_id: UUID del tema, en texto.
        nombre: Nombre del tema.
        subtemas: Subtemas ordenados por nombre normalizado.
    """

    tema_id: str
    nombre: str
    subtemas: tuple[SubtemaRespuesta, ...]


@dataclass(frozen=True, slots=True)
class CompetenciaRespuesta:
    """Competencia con sus temas y subtemas anidados (CONTRATOS.md 8.2).

    Attributes:
        competencia_id: UUID de la competencia, en texto.
        nombre: Nombre de la competencia.
        descripcion: Descripción, o ``None`` si no tiene.
        temas: Temas ordenados por nombre normalizado.
    """

    competencia_id: str
    nombre: str
    descripcion: str | None
    temas: tuple[TemaRespuesta, ...]


@dataclass(frozen=True, slots=True)
class ValidacionClasificacionRespuesta:
    """Resultado de validar una terna (CONTRATOS.md 6, ``ValidarClasificacionRespuesta``).

    Attributes:
        valida: Verdadero si la terna existe y es coherente.
        motivo: Primer motivo de rechazo, o ``NINGUNO``.
        detalle: Texto legible en español.
    """

    valida: bool
    motivo: MotivoRechazoClasificacion
    detalle: str


@dataclass(frozen=True, slots=True)
class SembrarCatalogoResultado:
    """Resultado de la siembra.

    Attributes:
        sembrado: ``True`` si el catálogo estaba vacío y se cargó; ``False`` si no se tocó.
        competencias_creadas: Cuántas competencias se crearon (0 si no se sembró).
    """

    sembrado: bool
    competencias_creadas: int
