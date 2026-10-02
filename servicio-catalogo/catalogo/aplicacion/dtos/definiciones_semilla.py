"""Definiciones con identificadores fijos que recibe la siembra del catálogo."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class DefinicionSubtema:
    """Subtema con identificador fijo.

    Attributes:
        id: UUID del subtema, en texto.
        nombre: Nombre del subtema, con sus tildes.
    """

    id: str
    nombre: str


@dataclass(frozen=True, slots=True)
class DefinicionTema:
    """Tema con identificador fijo y sus subtemas.

    Attributes:
        id: UUID del tema, en texto.
        nombre: Nombre del tema, con sus tildes.
        subtemas: Subtemas del tema.
    """

    id: str
    nombre: str
    subtemas: tuple[DefinicionSubtema, ...] = ()


@dataclass(frozen=True, slots=True)
class DefinicionCompetencia:
    """Competencia con identificador fijo y su jerarquía.

    Attributes:
        id: UUID de la competencia, en texto.
        nombre: Nombre de la competencia, con sus tildes.
        temas: Temas de la competencia.
        descripcion: Descripción opcional.
    """

    id: str
    nombre: str
    temas: tuple[DefinicionTema, ...] = ()
    descripcion: str | None = None


@dataclass(frozen=True, slots=True)
class SembrarCatalogoComando:
    """Definiciones que se cargan si el catálogo está vacío.

    Attributes:
        competencias: Competencias, con sus temas y subtemas, y ids fijos.
    """

    competencias: tuple[DefinicionCompetencia, ...]
