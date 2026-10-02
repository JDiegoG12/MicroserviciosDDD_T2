"""Modelos SQLAlchemy de las tablas ``competencias``, ``temas`` y ``subtemas``.

Son modelos de base de datos, no entidades de dominio (CONTRATOS.md 3.3.2): el mapeador los
traduce de ida y vuelta. El esquema lo crea la migración de Alembic, nunca el ORM.

* Las llaves foráneas protegen en la base la regla de no tener huérfanos (INV-23).
* Los índices únicos sobre el **nombre normalizado** respaldan INV-24: competencia en todo
  el catálogo, tema dentro de su competencia y subtema dentro de su tema.
"""

from __future__ import annotations

from uuid import UUID

from sqlalchemy import ForeignKey, String, UniqueConstraint, Uuid
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship

from catalogo.dominio.competencia import LONGITUD_MAXIMA_DESCRIPCION
from catalogo.dominio.nombre_catalogo import LONGITUD_MAXIMA_NOMBRE

UQ_COMPETENCIAS_NOMBRE = "uq_competencias_nombre_normalizado"
UQ_TEMAS_NOMBRE = "uq_temas_competencia_nombre_normalizado"
UQ_SUBTEMAS_NOMBRE = "uq_subtemas_tema_nombre_normalizado"
RESTRICCIONES_DE_NOMBRE = {UQ_COMPETENCIAS_NOMBRE, UQ_TEMAS_NOMBRE, UQ_SUBTEMAS_NOMBRE}
"""Nombres de las restricciones únicas que equivalen a ``NOMBRE_DUPLICADO``."""


class Base(DeclarativeBase):
    """Base declarativa de los modelos del catálogo."""


class CompetenciaModelo(Base):
    """Fila de ``competencias``."""

    __tablename__ = "competencias"
    __table_args__ = (UniqueConstraint("nombre_normalizado", name=UQ_COMPETENCIAS_NOMBRE),)

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True)
    nombre: Mapped[str] = mapped_column(String(LONGITUD_MAXIMA_NOMBRE))
    nombre_normalizado: Mapped[str] = mapped_column(String(LONGITUD_MAXIMA_NOMBRE))
    descripcion: Mapped[str | None] = mapped_column(String(LONGITUD_MAXIMA_DESCRIPCION))
    temas: Mapped[list[TemaModelo]] = relationship(
        back_populates="competencia", lazy="selectin", order_by="TemaModelo.nombre_normalizado"
    )


class TemaModelo(Base):
    """Fila de ``temas``."""

    __tablename__ = "temas"
    __table_args__ = (
        UniqueConstraint("competencia_id", "nombre_normalizado", name=UQ_TEMAS_NOMBRE),
    )

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True)
    competencia_id: Mapped[UUID] = mapped_column(
        Uuid, ForeignKey("competencias.id", name="fk_temas_competencia", ondelete="RESTRICT")
    )
    nombre: Mapped[str] = mapped_column(String(LONGITUD_MAXIMA_NOMBRE))
    nombre_normalizado: Mapped[str] = mapped_column(String(LONGITUD_MAXIMA_NOMBRE))
    competencia: Mapped[CompetenciaModelo] = relationship(back_populates="temas")
    subtemas: Mapped[list[SubtemaModelo]] = relationship(
        back_populates="tema", lazy="selectin", order_by="SubtemaModelo.nombre_normalizado"
    )


class SubtemaModelo(Base):
    """Fila de ``subtemas``."""

    __tablename__ = "subtemas"
    __table_args__ = (UniqueConstraint("tema_id", "nombre_normalizado", name=UQ_SUBTEMAS_NOMBRE),)

    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True)
    tema_id: Mapped[UUID] = mapped_column(
        Uuid, ForeignKey("temas.id", name="fk_subtemas_tema", ondelete="RESTRICT")
    )
    nombre: Mapped[str] = mapped_column(String(LONGITUD_MAXIMA_NOMBRE))
    nombre_normalizado: Mapped[str] = mapped_column(String(LONGITUD_MAXIMA_NOMBRE))
    tema: Mapped[TemaModelo] = relationship(back_populates="subtemas")
