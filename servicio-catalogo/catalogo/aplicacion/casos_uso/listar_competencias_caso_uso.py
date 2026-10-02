"""Caso de uso ``ListarCompetenciasCasoUso`` (D-11, lectura del catálogo)."""

from catalogo.aplicacion.dtos.mapeador_competencia import a_competencia_respuesta
from catalogo.aplicacion.dtos.respuestas import CompetenciaRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import ListarCompetenciasPuertoEntrada
from catalogo.aplicacion.seguridad.autorizacion import exigir_algun_rol
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio


class ListarCompetenciasCasoUso(ListarCompetenciasPuertoEntrada):
    """Lista el catálogo completo (``GET /competencias`` → 200, cualquier rol).

    No se pagina: el catálogo es pequeño y CONTRATOS.md 8.2 define una lista. Competencias,
    temas y subtemas salen ordenados por nombre normalizado, de forma ascendente.
    """

    def __init__(self, repositorio: CompetenciaRepositorio) -> None:
        """Crea el caso de uso.

        Args:
            repositorio: Repositorio de competencias.
        """
        self._repositorio = repositorio

    def ejecutar(self, usuario: UsuarioActual) -> list[CompetenciaRespuesta]:
        """Devuelve todas las competencias con sus temas y subtemas.

        Args:
            usuario: Quien llama; vale cualquier rol.

        Returns:
            Las competencias ordenadas por nombre normalizado.

        Raises:
            AccesoDenegadoExcepcion: Si el usuario no declara ningún rol.
        """
        exigir_algun_rol(usuario)
        competencias = sorted(
            self._repositorio.listar_todas(), key=lambda c: c.nombre.normalizado
        )
        return [a_competencia_respuesta(competencia) for competencia in competencias]
