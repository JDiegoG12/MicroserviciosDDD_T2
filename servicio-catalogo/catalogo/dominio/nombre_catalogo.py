"""Value object ``NombreCatalogo``: nombre de una competencia, un tema o un subtema."""

from __future__ import annotations

import unicodedata
from dataclasses import dataclass

from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion

LONGITUD_MAXIMA_NOMBRE = 120
"""Máximo de caracteres Unicode (puntos de código) de un nombre (CONTRATOS.md 4)."""


@dataclass(frozen=True, slots=True)
class NombreCatalogo:
    """Nombre de un elemento del catálogo, con su forma normalizada para comparar.

    Mide de 1 a 120 caracteres Unicode después de quitar los espacios del inicio y del
    final (CONTRATOS.md 4; ``len()`` cuenta puntos de código). El valor se conserva tal
    cual, con tildes: la forma normalizada solo se usa para comparar unicidad (INV-24).

    Attributes:
        valor: Nombre sin espacios al inicio ni al final.
    """

    valor: str

    def __post_init__(self) -> None:
        """Recorta el valor y valida su longitud.

        Raises:
            SolicitudInvalidaExcepcion: Si no es texto, queda vacío o supera 120 caracteres.
        """
        if not isinstance(self.valor, str):
            raise SolicitudInvalidaExcepcion("El nombre debe ser un texto.")
        recortado = self.valor.strip()
        if not recortado:
            raise SolicitudInvalidaExcepcion("El nombre no puede estar vacío.")
        if len(recortado) > LONGITUD_MAXIMA_NOMBRE:
            raise SolicitudInvalidaExcepcion(
                f"El nombre no puede superar {LONGITUD_MAXIMA_NOMBRE} caracteres."
            )
        object.__setattr__(self, "valor", recortado)

    @property
    def normalizado(self) -> str:
        """Forma para comparar unicidad (CONTRATOS.md 11.2, v1.8; INV-24).

        Minúsculas, sin tildes ni diéresis, sin espacios al inicio ni al final y con los
        espacios internos reducidos a uno. Así ``Estadística`` y ``  estadistica `` son
        el mismo nombre. La ``ñ`` se conserva como letra propia (``Año`` ≠ ``Ano``,
        CONTRATOS.md 11.2, v1.9).

        Returns:
            El nombre normalizado.
        """
        descompuesto = unicodedata.normalize("NFD", self.valor.lower())
        conservados: list[str] = []
        for caracter in descompuesto:
            if unicodedata.category(caracter) != "Mn" or _es_tilde_de_enie(conservados, caracter):
                conservados.append(caracter)
        recompuesto = unicodedata.normalize("NFC", "".join(conservados))
        return " ".join(recompuesto.split())

    def __str__(self) -> str:
        """Devuelve el nombre tal como se guardó.

        Returns:
            El nombre con sus tildes y mayúsculas originales.
        """
        return self.valor


def _es_tilde_de_enie(anteriores: list[str], marca: str) -> bool:
    """Indica si la marca combinante es la tilde de una ``n``, es decir, una ``ñ``.

    Args:
        anteriores: Caracteres ya conservados del texto descompuesto.
        marca: Marca combinante que se está evaluando.

    Returns:
        ``True`` si es la tilde (U+0303) justo después de una ``n``.
    """
    return marca == "\u0303" and bool(anteriores) and anteriores[-1] == "n"
