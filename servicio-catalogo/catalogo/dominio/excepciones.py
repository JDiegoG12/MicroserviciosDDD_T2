"""Excepciones del dominio del catálogo.

Cada excepción lleva el ``codigo`` exacto de CONTRATOS.md (secciones 5.3 y 8.2) para que
la etapa 2 las traduzca a HTTP o a gRPC sin ambigüedad.
"""


class CatalogoExcepcion(Exception):
    """Base de todas las excepciones con código del servicio de catálogo.

    Attributes:
        codigo: Código de error exacto de CONTRATOS.md (por ejemplo ``NOMBRE_DUPLICADO``).
        mensaje: Texto legible en español que explica el problema.
    """

    codigo: str = "ERROR_INTERNO"

    def __init__(self, mensaje: str) -> None:
        """Crea la excepción con su mensaje.

        Args:
            mensaje: Texto legible en español que explica el problema.
        """
        super().__init__(mensaje)
        self.mensaje = mensaje


class SolicitudInvalidaExcepcion(CatalogoExcepcion):
    """El dato de entrada viola su propio formato (CONTRATOS.md 5.3, regla 400).

    Se usa para nombres vacíos o de más de 120 caracteres, descripciones de más de
    500 caracteres e identificadores que no son un UUID. En gRPC se traduce a
    ``INVALID_ARGUMENT``.
    """

    codigo = "SOLICITUD_INVALIDA"


class NombreDuplicadoExcepcion(CatalogoExcepcion):
    """El nombre ya existe en su ámbito (INV-24, CONTRATOS.md 5.3, regla 409)."""

    codigo = "NOMBRE_DUPLICADO"


class TemaNoEncontradoExcepcion(CatalogoExcepcion):
    """El tema no existe en la competencia indicada (INV-23, CONTRATOS.md 5.3, regla 404)."""

    codigo = "TEMA_NO_ENCONTRADO"


class SubtemaNoEncontradoExcepcion(CatalogoExcepcion):
    """El subtema no existe en el tema indicado (INV-23, CONTRATOS.md 5.3, regla 404)."""

    codigo = "SUBTEMA_NO_ENCONTRADO"
