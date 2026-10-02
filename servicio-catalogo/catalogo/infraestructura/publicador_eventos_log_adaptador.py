"""Adaptador de ``PublicadorEventosPuerto`` que solo escribe los eventos en el log."""

from __future__ import annotations

import logging
from collections.abc import Sequence
from dataclasses import fields

from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.dominio.eventos import EventoDeDominio

registro = logging.getLogger("catalogo.eventos")


class PublicadorEventosLogAdaptador(PublicadorEventosPuerto):
    """Escribe cada evento de dominio en el log.

    El catálogo no publica al broker (CONTRATOS.md 1, tabla de comunicaciones): los eventos
    de dominio sirven de auditoría.
    """

    def publicar(self, eventos: Sequence[EventoDeDominio]) -> None:
        """Escribe una línea por evento: su nombre y sus datos como ``campo=valor``."""
        for evento in eventos:
            datos = " ".join(f"{campo.name}={getattr(evento, campo.name)}" for campo in fields(evento))
            registro.info("Evento de dominio %s %s", type(evento).__name__, datos)
