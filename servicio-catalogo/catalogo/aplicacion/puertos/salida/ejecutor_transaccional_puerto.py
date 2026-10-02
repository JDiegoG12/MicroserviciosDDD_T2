"""Puerto de salida ``EjecutorTransaccionalPuerto``."""

from abc import ABC, abstractmethod
from collections.abc import Callable

from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio


class EjecutorTransaccionalPuerto(ABC):
    """Ejecuta un caso de uso dentro de una transacción de la base de datos.

    Los casos de uso son síncronos y reciben su repositorio ya construido. Este puerto
    abre la transacción, le entrega al caso de uso un repositorio ligado a ella y la
    confirma si termina bien o la revierte si lanza una excepción. Así cada caso de uso
    es todo o nada, también la siembra (CONTRATOS.md 11.2, v1.9) y un caso de uso modifica
    un solo agregado por transacción (CONTRATOS.md 3.3.5).
    """

    @abstractmethod
    async def ejecutar[R](
        self, operacion: Callable[[CompetenciaRepositorio, PublicadorEventosPuerto], R]
    ) -> R:
        """Ejecuta la operación dentro de una transacción.

        Args:
            operacion: Función que construye el caso de uso con el repositorio y el
                publicador recibidos y lo ejecuta. Por ejemplo
                ``lambda repo, pub: CrearCompetenciaCasoUso(repo, pub).ejecutar(usuario, comando)``.

        Returns:
            Lo que devuelva la operación.

        Raises:
            BaseDeDatosNoDisponibleExcepcion: Si la base de datos no responde.
            CatalogoExcepcion: Cualquier excepción con código que lance el caso de uso (la
                transacción se revierte).
        """
