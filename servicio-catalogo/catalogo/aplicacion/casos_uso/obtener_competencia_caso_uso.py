"""Caso de uso ``ObtenerCompetenciaCasoUso`` (D-11, lectura del catálogo)."""

from catalogo.aplicacion.casos_uso.apoyo_casos_uso import cargar_competencia
from catalogo.aplicacion.dtos.comandos import ObtenerCompetenciaConsulta
from catalogo.aplicacion.dtos.mapeador_competencia import a_competencia_respuesta
from catalogo.aplicacion.dtos.respuestas import CompetenciaRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import ObtenerCompetenciaPuertoEntrada
from catalogo.aplicacion.seguridad.autorizacion import exigir_algun_rol
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio


class ObtenerCompetenciaCasoUso(ObtenerCompetenciaPuertoEntrada):
    """Consulta una competencia con su jerarquía (``GET /competencias/{competenciaId}`` → 200)."""

    def __init__(self, repositorio: CompetenciaRepositorio) -> None:
        """Crea el caso de uso.

        Args:
            repositorio: Repositorio de competencias.
        """
        self._repositorio = repositorio

    def ejecutar(
        self, usuario: UsuarioActual, consulta: ObtenerCompetenciaConsulta
    ) -> CompetenciaRespuesta:
        """Devuelve la competencia pedida.

        Args:
            usuario: Quien llama; vale cualquier rol.
            consulta: Identificador de la competencia.

        Returns:
            La competencia con sus temas y subtemas.

        Raises:
            AccesoDenegadoExcepcion: Si el usuario no declara ningún rol.
            SolicitudInvalidaExcepcion: Si el id está mal formado.
            CompetenciaNoEncontradaExcepcion: Si la competencia no existe.
        """
        exigir_algun_rol(usuario)
        competencia = cargar_competencia(self._repositorio, consulta.competencia_id)
        return a_competencia_respuesta(competencia)
