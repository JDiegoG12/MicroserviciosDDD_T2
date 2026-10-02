"""Caso de uso ``CrearCompetenciaCasoUso`` (D-11, INV-24)."""

from catalogo.aplicacion.dtos.comandos import CrearCompetenciaComando
from catalogo.aplicacion.dtos.mapeador_competencia import a_competencia_respuesta
from catalogo.aplicacion.dtos.respuestas import CompetenciaRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import CrearCompetenciaPuertoEntrada
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.aplicacion.seguridad.autorizacion import exigir_administrador
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.competencia import Competencia
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.dominio.verificador_nombre_competencia_servicio import (
    VerificadorNombreCompetenciaServicio,
)


class CrearCompetenciaCasoUso(CrearCompetenciaPuertoEntrada):
    """Crea una competencia nueva (``POST /competencias`` → 201).

    Reglas: D-11 (catálogo administrable por el ``ADMINISTRADOR``) e INV-24 (nombre único
    en el catálogo, comparando la forma normalizada).
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
        self, usuario: UsuarioActual, comando: CrearCompetenciaComando
    ) -> CompetenciaRespuesta:
        """Crea la competencia, la guarda y publica sus eventos.

        Args:
            usuario: Quien llama; necesita el rol ``ADMINISTRADOR``.
            comando: Nombre y descripción opcional.

        Returns:
            La competencia creada.

        Raises:
            AccesoDenegadoExcepcion: Si falta el rol ``ADMINISTRADOR``.
            SolicitudInvalidaExcepcion: Si el nombre o la descripción violan su formato.
            NombreDuplicadoExcepcion: Si el nombre normalizado ya existe en el catálogo.
        """
        exigir_administrador(usuario)
        nombre = NombreCatalogo(comando.nombre)
        competencia = Competencia.crear(nombre, comando.descripcion)
        self._verificador.exigir_nombre_disponible(nombre)
        self._repositorio.guardar(competencia)
        self._publicador.publicar(competencia.extraer_eventos())
        return a_competencia_respuesta(competencia)
