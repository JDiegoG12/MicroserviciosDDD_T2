"""Entidad ``Subtema``, interna del agregado ``Competencia``."""

from __future__ import annotations

from catalogo.dominio.identificadores import SubtemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo


class Subtema:
    """Subtema de un tema (nivel más específico del catálogo).

    Es una entidad interna: no se crea ni se modifica por fuera de ``Competencia``
    (CONTRATOS.md 11.2). Su identidad es ``id`` y no cambia al renombrarlo (INV-22).
    """

    def __init__(self, id: SubtemaId, nombre: NombreCatalogo) -> None:
        """Crea el subtema.

        Args:
            id: Identificador estable (INV-22).
            nombre: Nombre del subtema.
        """
        self._id = id
        self._nombre = nombre

    @property
    def id(self) -> SubtemaId:
        """Identificador estable del subtema (INV-22)."""
        return self._id

    @property
    def nombre(self) -> NombreCatalogo:
        """Nombre actual del subtema."""
        return self._nombre

    def _renombrar(self, nombre_nuevo: NombreCatalogo) -> None:
        """Cambia el nombre. Solo lo invoca ``Competencia``, que valida INV-24.

        Args:
            nombre_nuevo: Nombre nuevo.
        """
        self._nombre = nombre_nuevo
