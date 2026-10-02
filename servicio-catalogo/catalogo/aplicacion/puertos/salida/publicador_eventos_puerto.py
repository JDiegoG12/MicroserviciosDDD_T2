"""Puerto de salida ``PublicadorEventosPuerto``."""

from abc import ABC, abstractmethod
from collections.abc import Sequence

from catalogo.dominio.eventos import EventoDeDominio


class PublicadorEventosPuerto(ABC):
    """Entrega los eventos de dominio a quien los consuma.

    El caso de uso extrae los eventos del agregado después de guardarlo y se los pasa aquí.
    En la etapa 2 su adaptador solo los escribe en el log, porque Catálogo no publica al
    broker (CONTRATOS.md 1, tabla de comunicaciones).
    """

    @abstractmethod
    def publicar(self, eventos: Sequence[EventoDeDominio]) -> None:
        """Publica los eventos en el orden recibido.

        Args:
            eventos: Eventos de dominio ya confirmados (el agregado ya está guardado).
        """
