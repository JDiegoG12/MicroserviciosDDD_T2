"""Interfaz de repositorio del agregado ``Competencia`` (MODELO-DOMINIO.md 12.3)."""

from __future__ import annotations

from abc import ABC, abstractmethod

from catalogo.dominio.competencia import Competencia
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId


class CompetenciaRepositorio(ABC):
    """Persistencia del agregado ``Competencia`` con su jerarquía completa.

    La implementación (etapa 2) vive en ``infraestructura``. Guarda y carga la competencia
    junto con sus temas y subtemas como una unidad. Los métodos ``existe_*`` consultan todo
    el catálogo, porque la unicidad de competencias y la validación de clasificaciones
    cruzan agregados (INV-24, INV-07).
    """

    @abstractmethod
    def guardar(self, competencia: Competencia) -> None:
        """Guarda una competencia nueva o actualiza una existente, con todos sus temas y subtemas.

        Args:
            competencia: Agregado a guardar.
        """

    @abstractmethod
    def obtener_por_id(self, competencia_id: CompetenciaId) -> Competencia | None:
        """Busca una competencia por su identificador.

        Args:
            competencia_id: Identificador de la competencia.

        Returns:
            La competencia con su jerarquía completa, o ``None`` si no existe.
        """

    @abstractmethod
    def listar_todas(self) -> list[Competencia]:
        """Lista todas las competencias del catálogo (sin orden garantizado).

        Returns:
            Todas las competencias con su jerarquía completa.
        """

    @abstractmethod
    def esta_vacio(self) -> bool:
        """Indica si el catálogo no tiene ninguna competencia (lo usa la siembra, CONTRATOS 11.2).

        Returns:
            ``True`` si no hay competencias.
        """

    @abstractmethod
    def existe_competencia_con_nombre(
        self, nombre_normalizado: str, excluir_id: CompetenciaId | None = None
    ) -> bool:
        """Indica si otra competencia tiene ese nombre normalizado (INV-24).

        Args:
            nombre_normalizado: Resultado de ``NombreCatalogo.normalizado``.
            excluir_id: Competencia que no cuenta como conflicto (la que se está renombrando).

        Returns:
            ``True`` si existe una competencia distinta de ``excluir_id`` con ese nombre.
        """

    @abstractmethod
    def existe_tema(self, tema_id: TemaId) -> bool:
        """Indica si el tema existe en alguna competencia del catálogo (INV-07).

        Args:
            tema_id: Identificador del tema.

        Returns:
            ``True`` si algún tema del catálogo tiene ese identificador.
        """

    @abstractmethod
    def existe_subtema(self, subtema_id: SubtemaId) -> bool:
        """Indica si el subtema existe en algún tema del catálogo (INV-07).

        Args:
            subtema_id: Identificador del subtema.

        Returns:
            ``True`` si algún subtema del catálogo tiene ese identificador.
        """
