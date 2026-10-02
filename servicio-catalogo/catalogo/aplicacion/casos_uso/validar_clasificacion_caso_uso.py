"""Caso de uso ``ValidarClasificacionCasoUso`` (INV-07, D-13)."""

from catalogo.aplicacion.dtos.comandos import ValidarClasificacionConsulta
from catalogo.aplicacion.dtos.respuestas import ValidacionClasificacionRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import ValidarClasificacionPuertoEntrada
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.validador_clasificacion_servicio import ValidadorClasificacionServicio


class ValidarClasificacionCasoUso(ValidarClasificacionPuertoEntrada):
    """Responde la consulta gRPC ``CatalogoAcademico.ValidarClasificacion`` (CONTRATOS.md 6).

    Es una llamada interna entre servicios, así que no exige rol. Una terna inválida no es
    una excepción: se responde con ``valida=False`` y el motivo. Un id que no es UUID sí es
    un error de entrada (``SOLICITUD_INVALIDA``, en gRPC ``INVALID_ARGUMENT``).
    """

    def __init__(self, repositorio: CompetenciaRepositorio) -> None:
        """Crea el caso de uso.

        Args:
            repositorio: Repositorio de competencias.
        """
        self._validador = ValidadorClasificacionServicio(repositorio)

    def ejecutar(self, consulta: ValidarClasificacionConsulta) -> ValidacionClasificacionRespuesta:
        """Valida la terna aplicando el orden de verificación de ``MotivoRechazo``.

        Args:
            consulta: Los tres identificadores, en texto.

        Returns:
            El resultado, con el primer motivo de rechazo si no es válida.

        Raises:
            SolicitudInvalidaExcepcion: Si algún id no es un UUID bien formado.
        """
        competencia_id = CompetenciaId.desde_texto(consulta.competencia_id)
        tema_id = TemaId.desde_texto(consulta.tema_id)
        subtema_id = SubtemaId.desde_texto(consulta.subtema_id)
        resultado = self._validador.validar(competencia_id, tema_id, subtema_id)
        return ValidacionClasificacionRespuesta(
            valida=resultado.valida, motivo=resultado.motivo, detalle=resultado.detalle
        )
