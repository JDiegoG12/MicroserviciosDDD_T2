"""Crea las tablas del catálogo: competencias, temas y subtemas.

Revision ID: 0001
Revises:
Create Date: 2026-10-02

INV-23: las llaves foráneas impiden temas sin competencia y subtemas sin tema.
INV-24: los índices únicos sobre el nombre normalizado respaldan la unicidad de nombres
(competencia en todo el catálogo, tema en su competencia, subtema en su tema).
La siembra de la tabla 4.3 NO va aquí: la hace SembrarCatalogoCasoUso al arrancar.
Nunca se borra nada: las llaves foráneas son ``ON DELETE RESTRICT`` y no hay DELETE.
"""

import sqlalchemy as sa
from alembic import op

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    """Crea competencias, temas y subtemas con sus llaves foráneas e índices únicos."""
    op.create_table(
        "competencias",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("nombre", sa.String(length=120), nullable=False),
        sa.Column("nombre_normalizado", sa.String(length=120), nullable=False),
        sa.Column("descripcion", sa.String(length=500), nullable=True),
        sa.PrimaryKeyConstraint("id", name="pk_competencias"),
        sa.UniqueConstraint("nombre_normalizado", name="uq_competencias_nombre_normalizado"),
    )
    op.create_table(
        "temas",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("competencia_id", sa.Uuid(), nullable=False),
        sa.Column("nombre", sa.String(length=120), nullable=False),
        sa.Column("nombre_normalizado", sa.String(length=120), nullable=False),
        sa.PrimaryKeyConstraint("id", name="pk_temas"),
        sa.ForeignKeyConstraint(
            ["competencia_id"], ["competencias.id"], name="fk_temas_competencia", ondelete="RESTRICT"
        ),
        sa.UniqueConstraint(
            "competencia_id", "nombre_normalizado", name="uq_temas_competencia_nombre_normalizado"
        ),
    )
    op.create_table(
        "subtemas",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("tema_id", sa.Uuid(), nullable=False),
        sa.Column("nombre", sa.String(length=120), nullable=False),
        sa.Column("nombre_normalizado", sa.String(length=120), nullable=False),
        sa.PrimaryKeyConstraint("id", name="pk_subtemas"),
        sa.ForeignKeyConstraint(
            ["tema_id"], ["temas.id"], name="fk_subtemas_tema", ondelete="RESTRICT"
        ),
        sa.UniqueConstraint(
            "tema_id", "nombre_normalizado", name="uq_subtemas_tema_nombre_normalizado"
        ),
    )


def downgrade() -> None:
    """Elimina las tablas (solo para desarrollo local; en producción no se usa)."""
    op.drop_table("subtemas")
    op.drop_table("temas")
    op.drop_table("competencias")
