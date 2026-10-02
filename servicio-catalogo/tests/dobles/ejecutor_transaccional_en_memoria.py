"""Doble en memoria de ``EjecutorTransaccionalPuerto``: sin base de datos."""

from collections.abc import Callable

from catalogo.aplicacion.puertos.salida.ejecutor_transaccional_puerto import (
    EjecutorTransaccionalPuerto,
)
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.infraestructura.correlacion import obtener_id_correlacion
from tests.dobles.competencia_repositorio_en_memoria import CompetenciaRepositorioEnMemoria
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria


class EjecutorTransaccionalEnMemoria(EjecutorTransaccionalPuerto):
    """Ejecuta la operación directamente con un repositorio y un publicador en memoria.

    Recuerda la correlación activa en cada ejecución para poder comprobar que llegó desde
    el encabezado o el metadato.
    """

    def __init__(
        self,
        repositorio: CompetenciaRepositorioEnMemoria,
        publicador: PublicadorEventosEnMemoria,
    ) -> None:
        """Crea el ejecutor.

        Args:
            repositorio: Repositorio en memoria que se entrega a las operaciones.
            publicador: Publicador en memoria que se entrega a las operaciones.
        """
        self.repositorio = repositorio
        self.publicador = publicador
        self.correlaciones_vistas: list[str] = []
        self.falla: Exception | None = None

    async def ejecutar[R](
        self, operacion: Callable[[CompetenciaRepositorio, PublicadorEventosPuerto], R]
    ) -> R:
        """Ejecuta la operación; si ``falla`` está definida, la lanza en su lugar."""
        self.correlaciones_vistas.append(obtener_id_correlacion())
        if self.falla is not None:
            raise self.falla
        return operacion(self.repositorio, self.publicador)
