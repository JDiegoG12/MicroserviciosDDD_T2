"""Doble en memoria de ``CompetenciaRepositorio``."""

from __future__ import annotations

import copy

from catalogo.dominio.competencia import Competencia
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId


class CompetenciaRepositorioEnMemoria(CompetenciaRepositorio):
    """Guarda copias de las competencias en un diccionario.

    Copia al guardar y al leer, como lo haría una base de datos: un cambio hecho en un
    agregado y no guardado no se ve en lecturas posteriores. Los eventos pendientes no se
    persisten, igual que en la base real.
    """

    def __init__(self) -> None:
        """Crea el repositorio vacío."""
        self._competencias: dict[CompetenciaId, Competencia] = {}
        self.cantidad_de_guardados = 0

    def guardar(self, competencia: Competencia) -> None:
        """Guarda una copia de la competencia, sin sus eventos pendientes."""
        copia = copy.deepcopy(competencia)
        copia.extraer_eventos()
        self._competencias[competencia.id] = copia
        self.cantidad_de_guardados += 1

    def obtener_por_id(self, competencia_id: CompetenciaId) -> Competencia | None:
        """Devuelve una copia de la competencia, o ``None`` si no existe."""
        competencia = self._competencias.get(competencia_id)
        return copy.deepcopy(competencia) if competencia is not None else None

    def listar_todas(self) -> list[Competencia]:
        """Devuelve copias de todas las competencias, en orden de inserción."""
        return [copy.deepcopy(c) for c in self._competencias.values()]

    def esta_vacio(self) -> bool:
        """Indica si no hay competencias guardadas."""
        return not self._competencias

    def existe_competencia_con_nombre(
        self, nombre_normalizado: str, excluir_id: CompetenciaId | None = None
    ) -> bool:
        """Indica si otra competencia tiene ese nombre normalizado."""
        return any(
            c.nombre.normalizado == nombre_normalizado and c.id != excluir_id
            for c in self._competencias.values()
        )

    def existe_tema(self, tema_id: TemaId) -> bool:
        """Indica si algún tema del catálogo tiene ese identificador."""
        return any(c.buscar_tema(tema_id) is not None for c in self._competencias.values())

    def existe_subtema(self, subtema_id: SubtemaId) -> bool:
        """Indica si algún subtema del catálogo tiene ese identificador."""
        return any(
            t.buscar_subtema(subtema_id) is not None
            for c in self._competencias.values()
            for t in c.temas
        )
