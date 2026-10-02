"""Servicio de dominio ``VerificadorNombreCompetenciaServicio``."""

from __future__ import annotations

from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.excepciones import NombreDuplicadoExcepcion
from catalogo.dominio.identificadores import CompetenciaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo


class VerificadorNombreCompetenciaServicio:
    """Garantiza que el nombre de una competencia sea único en todo el catálogo (INV-24).

    La unicidad entre competencias cruza agregados, por eso no puede protegerla la propia
    ``Competencia``: se resuelve aquí consultando el repositorio (MODELO-DOMINIO.md A.2 y A.3).
    """

    def __init__(self, repositorio: CompetenciaRepositorio) -> None:
        """Crea el verificador.

        Args:
            repositorio: Repositorio de competencias que se consulta.
        """
        self._repositorio = repositorio

    def exigir_nombre_disponible(
        self, nombre: NombreCatalogo, competencia_id: CompetenciaId | None = None
    ) -> None:
        """Rechaza el nombre si otra competencia ya lo usa (comparando la forma normalizada).

        Renombrar una competencia con su mismo nombre, o con una variante que se normaliza
        igual, no es duplicado (CONTRATOS.md 11.2): por eso se excluye a la propia competencia.

        Args:
            nombre: Nombre que se quiere usar.
            competencia_id: Competencia que se está renombrando, o ``None`` si es nueva.

        Raises:
            NombreDuplicadoExcepcion: INV-24, si otra competencia tiene ese nombre normalizado.
        """
        if self._repositorio.existe_competencia_con_nombre(nombre.normalizado, competencia_id):
            raise NombreDuplicadoExcepcion(
                f"Ya existe una competencia llamada '{nombre}' en el catálogo."
            )
