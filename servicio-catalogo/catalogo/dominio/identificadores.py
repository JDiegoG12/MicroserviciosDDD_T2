"""Value objects de identidad del catálogo.

INV-22 / D-13: el identificador de una competencia, un tema o un subtema es estable e
independiente de su nombre. Se representa como un UUID en texto minúsculo (CONTRATOS.md 4).
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Self
from uuid import UUID, uuid4

from catalogo.dominio.excepciones import SolicitudInvalidaExcepcion


@dataclass(frozen=True, slots=True)
class _IdentificadorUuid:
    """Base de los identificadores: envuelve un UUID y lo valida al construirse.

    Attributes:
        valor: UUID que identifica al elemento.
    """

    valor: UUID

    def __post_init__(self) -> None:
        """Valida que el valor sea un UUID.

        Raises:
            SolicitudInvalidaExcepcion: Si el valor no es un ``uuid.UUID``.
        """
        if not isinstance(self.valor, UUID):
            raise SolicitudInvalidaExcepcion(
                f"El identificador debe ser un UUID, pero se recibió {self.valor!r}."
            )

    @classmethod
    def generar(cls) -> Self:
        """Genera un identificador nuevo (UUID v4).

        Returns:
            Un identificador único.
        """
        return cls(uuid4())

    @classmethod
    def desde_texto(cls, texto: str) -> Self:
        """Construye el identificador a partir de su texto.

        Se acepta solo la forma canónica con guiones (se tolera la mayúscula y se
        normaliza a minúscula), porque CONTRATOS.md 4 fija el UUID en texto minúsculo.

        Args:
            texto: UUID en texto, por ejemplo ``22222222-2222-4222-8222-000000000101``.

        Returns:
            El identificador.

        Raises:
            SolicitudInvalidaExcepcion: Si el texto no es un UUID bien formado.
        """
        if not isinstance(texto, str):
            raise SolicitudInvalidaExcepcion(
                f"El identificador debe ser un texto UUID, pero se recibió {texto!r}."
            )
        # CONTRATOS 4 (v1.9): solo la forma canónica de 36 caracteres con guiones; la
        # mayúscula se normaliza y cualquier otra forma es SOLICITUD_INVALIDA.
        candidato = texto.strip().lower()
        try:
            valor = UUID(candidato)
        except ValueError:
            raise SolicitudInvalidaExcepcion(
                f"El identificador '{texto}' no es un UUID bien formado."
            ) from None
        if str(valor) != candidato:
            raise SolicitudInvalidaExcepcion(
                f"El identificador '{texto}' no tiene la forma canónica de un UUID."
            )
        return cls(valor)

    def __str__(self) -> str:
        """Devuelve el UUID en texto minúsculo con guiones.

        Returns:
            El UUID en texto.
        """
        return str(self.valor)


@dataclass(frozen=True, slots=True)
class CompetenciaId(_IdentificadorUuid):
    """Identificador estable de una ``Competencia`` (INV-22)."""


@dataclass(frozen=True, slots=True)
class TemaId(_IdentificadorUuid):
    """Identificador estable de un ``Tema`` (INV-22)."""


@dataclass(frozen=True, slots=True)
class SubtemaId(_IdentificadorUuid):
    """Identificador estable de un ``Subtema`` (INV-22)."""
