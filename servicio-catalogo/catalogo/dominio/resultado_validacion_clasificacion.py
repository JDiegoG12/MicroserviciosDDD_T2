"""Value object ``ResultadoValidacionClasificacion``."""

from __future__ import annotations

from dataclasses import dataclass

from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion


@dataclass(frozen=True, slots=True)
class ResultadoValidacionClasificacion:
    """Resultado de validar una terna Competencia/Tema/Subtema (INV-07, D-13).

    Una clasificación inválida no es un error: se devuelve ``valida=False`` con el motivo
    (CONTRATOS.md 6). Es coherente por construcción: ``valida`` es verdadero si y solo si
    el motivo es ``NINGUNO``.

    Attributes:
        valida: Verdadero si la terna existe y es coherente.
        motivo: Primer motivo de rechazo, o ``NINGUNO`` si es válida.
        detalle: Texto legible en español para mostrar al usuario.
    """

    valida: bool
    motivo: MotivoRechazoClasificacion
    detalle: str

    def __post_init__(self) -> None:
        """Comprueba que ``valida`` y ``motivo`` sean coherentes.

        Raises:
            ValueError: Si ``valida`` y ``motivo`` se contradicen (error de programación).
        """
        sin_motivo = self.motivo is MotivoRechazoClasificacion.NINGUNO
        if self.valida != sin_motivo:
            raise ValueError("Una clasificación es válida si y solo si su motivo es NINGUNO.")

    @classmethod
    def aceptada(cls) -> ResultadoValidacionClasificacion:
        """Construye el resultado de una clasificación válida.

        Returns:
            Resultado con ``valida=True`` y motivo ``NINGUNO``.
        """
        return cls(
            valida=True,
            motivo=MotivoRechazoClasificacion.NINGUNO,
            detalle="La clasificación es válida.",
        )

    @classmethod
    def rechazada(
        cls, motivo: MotivoRechazoClasificacion, detalle: str
    ) -> ResultadoValidacionClasificacion:
        """Construye el resultado de una clasificación inválida.

        Args:
            motivo: Primer motivo encontrado (distinto de ``NINGUNO``).
            detalle: Texto legible en español que explica el rechazo.

        Returns:
            Resultado con ``valida=False``.

        Raises:
            ValueError: Si el motivo es ``NINGUNO`` (error de programación).
        """
        return cls(valida=False, motivo=motivo, detalle=detalle)
