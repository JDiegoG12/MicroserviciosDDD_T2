"""Caso de uso ``RenombrarCompetenciaCasoUso`` (D-11, INV-22, INV-24)."""

from catalogo.aplicacion.casos_uso.apoyo_casos_uso import cargar_competencia
from catalogo.aplicacion.dtos.comandos import RenombrarCompetenciaComando
from catalogo.aplicacion.dtos.mapeador_competencia import a_competencia_respuesta
from catalogo.aplicacion.dtos.respuestas import CompetenciaRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import (
    RenombrarCompetenciaPuertoEntrada,
)
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.aplicacion.seguridad.autorizacion import exigir_administrador
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.dominio.verificador_nombre_competencia_servicio import (
    VerificadorNombreCompetenciaServicio,
)


class RenombrarCompetenciaCasoUso(RenombrarCompetenciaPuertoEntrada):
    """Renombra una competencia (``PUT /competencias/{competenciaId}`` → 200).

    Reglas: INV-22 (el identificador no cambia) e INV-24 (nombre único; renombrar con el
    mismo nombre o con una variante que se normaliza igual no es duplicado).
    """

    def __init__(
        self, repositorio: CompetenciaRepositorio, publicador: PublicadorEventosPuerto
    ) -> None:
        """Crea el caso de uso.

        Args:
            repositorio: Repositorio de competencias.
            publicador: Puerto que recibe los eventos de dominio.
        """
        self._repositorio = repositorio
        self._publicador = publicador
        self._verificador = VerificadorNombreCompetenciaServicio(repositorio)

    def ejecutar(
        self, usuario: UsuarioActual, comando: RenombrarCompetenciaComando
    ) -> CompetenciaRespuesta:
        """Renombra la competencia, la guarda y publica sus eventos.

        Args:
            usuario: Quien llama; necesita el rol ``ADMINISTRADOR``.
            comando: Competencia, nombre nuevo y descripción opcional.

        Returns:
            La competencia actualizada.

        Raises:
            AccesoDenegadoExcepcion: Si falta el rol ``ADMINISTRADOR``.
            SolicitudInvalidaExcepcion: Si el id, el nombre o la descripción son inválidos.
            CompetenciaNoEncontradaExcepcion: Si la competencia no existe.
            NombreDuplicadoExcepcion: Si otra competencia tiene ese nombre normalizado.
        """
        exigir_administrador(usuario)
        nombre = NombreCatalogo(comando.nombre)
        competencia = cargar_competencia(self._repositorio, comando.competencia_id)
        self._verificador.exigir_nombre_disponible(nombre, competencia.id)
        competencia.renombrar(nombre, comando.descripcion)
        self._repositorio.guardar(competencia)
        self._publicador.publicar(competencia.extraer_eventos())
        return a_competencia_respuesta(competencia)
