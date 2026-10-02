"""Implementación de ``CompetenciaRepositorio`` con SQLAlchemy sobre PostgreSQL."""

from __future__ import annotations

from sqlalchemy import exists, select
from sqlalchemy.orm import Session

from catalogo.dominio.competencia import Competencia
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.infraestructura.base_datos.mapeador_competencia import a_dominio, aplicar_a_fila
from catalogo.infraestructura.base_datos.modelos import (
    CompetenciaModelo,
    SubtemaModelo,
    TemaModelo,
)


class CompetenciaRepositorioSqlAlchemy(CompetenciaRepositorio):
    """Guarda y carga el agregado ``Competencia`` completo (con sus temas y subtemas).

    Recibe una ``Session`` de estilo síncrono. Se ejecuta dentro de
    ``AsyncSession.run_sync`` (ver ``EjecutorTransaccionalSqlAlchemy``), de modo que el
    motor es asíncrono y los casos de uso de la etapa 1 siguen siendo síncronos. La
    transacción no la maneja el repositorio: la confirma o revierte el ejecutor.
    """

    def __init__(self, sesion: Session) -> None:
        """Crea el repositorio.

        Args:
            sesion: Sesión ligada a la transacción en curso.
        """
        self._sesion = sesion

    def guardar(self, competencia: Competencia) -> None:
        """Inserta o actualiza la competencia con todos sus temas y subtemas.

        La escritura se envía a la sesión con ``flush``; el commit lo hace la transacción que abre
        ``EjecutorTransaccionalSqlAlchemy`` alrededor del caso de uso.

        Args:
            competencia: Agregado ``Competencia`` completo que se va a guardar, con su jerarquía de
                temas y subtemas.
        """
        fila = self._sesion.get(CompetenciaModelo, competencia.id.valor)
        if fila is None:
            fila = CompetenciaModelo(id=competencia.id.valor)
            self._sesion.add(fila)
        aplicar_a_fila(competencia, fila)
        self._sesion.flush()

    def obtener_por_id(self, competencia_id: CompetenciaId) -> Competencia | None:
        """Carga la competencia con su jerarquía, o ``None`` si no existe."""
        fila = self._sesion.get(CompetenciaModelo, competencia_id.valor)
        return a_dominio(fila) if fila is not None else None

    def listar_todas(self) -> list[Competencia]:
        """Carga todas las competencias con su jerarquía, ordenadas por nombre normalizado."""
        filas = self._sesion.scalars(
            select(CompetenciaModelo).order_by(CompetenciaModelo.nombre_normalizado)
        ).all()
        return [a_dominio(fila) for fila in filas]

    def esta_vacio(self) -> bool:
        """Indica si no hay ninguna competencia."""
        return not self._sesion.scalar(select(exists().select_from(CompetenciaModelo)))

    def existe_competencia_con_nombre(
        self, nombre_normalizado: str, excluir_id: CompetenciaId | None = None
    ) -> bool:
        """Indica si otra competencia tiene ese nombre normalizado (INV-24)."""
        condicion = CompetenciaModelo.nombre_normalizado == nombre_normalizado
        if excluir_id is not None:
            condicion = condicion & (CompetenciaModelo.id != excluir_id.valor)
        return bool(self._sesion.scalar(select(exists().where(condicion))))

    def existe_tema(self, tema_id: TemaId) -> bool:
        """Indica si algún tema del catálogo tiene ese identificador (INV-07)."""
        return bool(self._sesion.scalar(select(exists().where(TemaModelo.id == tema_id.valor))))

    def existe_subtema(self, subtema_id: SubtemaId) -> bool:
        """Indica si algún subtema del catálogo tiene ese identificador (INV-07)."""
        return bool(
            self._sesion.scalar(select(exists().where(SubtemaModelo.id == subtema_id.valor)))
        )
