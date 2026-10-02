"""Servicio de dominio ``ValidadorClasificacionServicio``."""

from __future__ import annotations

from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.motivo_rechazo_clasificacion import MotivoRechazoClasificacion
from catalogo.dominio.resultado_validacion_clasificacion import ResultadoValidacionClasificacion


class ValidadorClasificacionServicio:
    """Verifica que una terna Competencia/Tema/Subtema exista y sea coherente (INV-07, D-13).

    Implementa la consulta del *Open Host Service* de CONTRATOS.md sección 6. Una
    clasificación inválida no es una excepción: se devuelve con ``valida=False`` y el
    primer motivo, en este orden exacto:

    1. La competencia no existe → ``COMPETENCIA_INEXISTENTE``.
    2. El tema no está en esa competencia: si existe en otra → ``TEMA_NO_PERTENECE_A_COMPETENCIA``;
       si no existe en ninguna → ``TEMA_INEXISTENTE``.
    3. El subtema no está en ese tema: si existe en otro → ``SUBTEMA_NO_PERTENECE_A_TEMA``;
       si no existe en ninguno → ``SUBTEMA_INEXISTENTE``.
    4. Todo bien → ``valida=True`` y motivo ``NINGUNO``.

    Los identificadores mal formados no llegan aquí: son un error de entrada
    (``SOLICITUD_INVALIDA``) que se detecta al construir los identificadores.
    """

    def __init__(self, repositorio: CompetenciaRepositorio) -> None:
        """Crea el validador.

        Args:
            repositorio: Repositorio de competencias que se consulta.
        """
        self._repositorio = repositorio

    def validar(
        self, competencia_id: CompetenciaId, tema_id: TemaId, subtema_id: SubtemaId
    ) -> ResultadoValidacionClasificacion:
        """Valida la terna aplicando el orden de verificación del enum ``MotivoRechazo``.

        Args:
            competencia_id: Competencia de la clasificación.
            tema_id: Tema de la clasificación.
            subtema_id: Subtema de la clasificación.

        Returns:
            El resultado, con el primer motivo de rechazo si no es válida.
        """
        competencia = self._repositorio.obtener_por_id(competencia_id)
        if competencia is None:
            return ResultadoValidacionClasificacion.rechazada(
                MotivoRechazoClasificacion.COMPETENCIA_INEXISTENTE,
                f"La competencia {competencia_id} no existe en el catálogo.",
            )

        tema = competencia.buscar_tema(tema_id)
        if tema is None:
            if self._repositorio.existe_tema(tema_id):
                return ResultadoValidacionClasificacion.rechazada(
                    MotivoRechazoClasificacion.TEMA_NO_PERTENECE_A_COMPETENCIA,
                    f"El tema {tema_id} no pertenece a la competencia {competencia_id}.",
                )
            return ResultadoValidacionClasificacion.rechazada(
                MotivoRechazoClasificacion.TEMA_INEXISTENTE,
                f"El tema {tema_id} no existe en el catálogo.",
            )

        if tema.buscar_subtema(subtema_id) is None:
            if self._repositorio.existe_subtema(subtema_id):
                return ResultadoValidacionClasificacion.rechazada(
                    MotivoRechazoClasificacion.SUBTEMA_NO_PERTENECE_A_TEMA,
                    f"El subtema {subtema_id} no pertenece al tema {tema_id}.",
                )
            return ResultadoValidacionClasificacion.rechazada(
                MotivoRechazoClasificacion.SUBTEMA_INEXISTENTE,
                f"El subtema {subtema_id} no existe en el catálogo.",
            )

        return ResultadoValidacionClasificacion.aceptada()
