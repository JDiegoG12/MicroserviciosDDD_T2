"""Marcador para distinguir un dato que no se envió de un dato enviado como ``None``."""

from enum import Enum


class SinCambio(Enum):
    """Valor único que significa "el campo no vino en la solicitud: conservar el actual".

    CONTRATOS.md 8.2 (v1.9): en el ``PUT`` de una competencia, si ``descripcion`` no viene
    se conserva la actual; si viene como ``null`` o ``""`` se borra; si viene con texto se
    reemplaza. ``None`` ya significa "borrar", por eso hace falta otro marcador.
    """

    SIN_CAMBIO = 0


SIN_CAMBIO = SinCambio.SIN_CAMBIO
"""Instancia única de ``SinCambio``."""
