"""Doble en memoria de ``PublicadorEventosPuerto``."""

from collections.abc import Sequence

from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.dominio.eventos import EventoDeDominio


class PublicadorEventosEnMemoria(PublicadorEventosPuerto):
    """Recuerda los eventos publicados, en orden, para poder comprobarlos."""

    def __init__(self) -> None:
        """Crea el publicador sin eventos."""
        self.eventos_publicados: list[EventoDeDominio] = []

    def publicar(self, eventos: Sequence[EventoDeDominio]) -> None:
        """Agrega los eventos a la lista de publicados."""
        self.eventos_publicados.extend(eventos)
