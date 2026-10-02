"""Datos semilla del catálogo: tabla 4.3 de CONTRATOS.md, con ids y nombres exactos.

Son datos puros. La etapa 2 los pasa a ``SembrarCatalogoCasoUso`` al arrancar el servicio.
Los nombres se guardan tal cual, con sus tildes; la normalización solo se usa para comparar.
"""

from catalogo.aplicacion.dtos.definiciones_semilla import (
    DefinicionCompetencia,
    DefinicionSubtema,
    DefinicionTema,
)

CATALOGO_SEMILLA: tuple[DefinicionCompetencia, ...] = (
    DefinicionCompetencia(
        id="22222222-2222-4222-8222-000000000101",
        nombre="Razonamiento cuantitativo",
        temas=(
            DefinicionTema(
                id="22222222-2222-4222-8222-000000000201",
                nombre="Estadística",
                subtemas=(
                    DefinicionSubtema(
                        "22222222-2222-4222-8222-000000000301", "Medidas de tendencia central"
                    ),
                    DefinicionSubtema("22222222-2222-4222-8222-000000000302", "Probabilidad"),
                ),
            ),
            DefinicionTema(
                id="22222222-2222-4222-8222-000000000202",
                nombre="Álgebra",
                subtemas=(
                    DefinicionSubtema("22222222-2222-4222-8222-000000000303", "Ecuaciones lineales"),
                ),
            ),
        ),
    ),
    DefinicionCompetencia(
        id="22222222-2222-4222-8222-000000000102",
        nombre="Diseño de software",
        temas=(
            DefinicionTema(
                id="22222222-2222-4222-8222-000000000203",
                nombre="Patrones de diseño",
                subtemas=(
                    DefinicionSubtema("22222222-2222-4222-8222-000000000304", "Patrones creacionales"),
                ),
            ),
            DefinicionTema(
                id="22222222-2222-4222-8222-000000000204",
                nombre="Arquitectura de software",
                subtemas=(
                    DefinicionSubtema("22222222-2222-4222-8222-000000000305", "Microservicios"),
                ),
            ),
        ),
    ),
)
