"""Enum ``MotivoRechazoClasificacion``, espejo de ``MotivoRechazo`` del ``.proto``."""

from enum import Enum


class MotivoRechazoClasificacion(Enum):
    """Primer motivo por el que una terna Competencia/Tema/Subtema no es válida.

    Los valores y su orden son los de ``MotivoRechazo`` en CONTRATOS.md sección 6 (sin el
    prefijo ``MOTIVO_RECHAZO_``), que es también el orden en que se evalúa (INV-07, D-13).
    """

    NINGUNO = 0
    COMPETENCIA_INEXISTENTE = 1
    TEMA_INEXISTENTE = 2
    TEMA_NO_PERTENECE_A_COMPETENCIA = 3
    SUBTEMA_INEXISTENTE = 4
    SUBTEMA_NO_PERTENECE_A_TEMA = 5
