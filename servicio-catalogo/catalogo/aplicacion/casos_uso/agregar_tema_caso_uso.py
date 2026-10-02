"""Caso de uso ``AgregarTemaCasoUso`` (D-11, INV-23, INV-24)."""

from catalogo.aplicacion.casos_uso.apoyo_casos_uso import cargar_competencia
from catalogo.aplicacion.dtos.comandos import AgregarTemaComando
from catalogo.aplicacion.dtos.mapeador_competencia import a_competencia_respuesta
from catalogo.aplicacion.dtos.respuestas import CompetenciaRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import AgregarTemaPuertoEntrada
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.aplicacion.seguridad.autorizacion import exigir_administrador
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.nombre_catalogo import NombreCatalogo


class AgregarTemaCasoUso(AgregarTemaPuertoEntrada):
    """Agrega un tema a una competencia (``POST /competencias/{competenciaId}/temas`` → 201).

    Reglas: INV-23 (el tema nace dentro de una competencia existente) e INV-24 (nombre de
    tema único dentro de su competencia).
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

    def ejecutar(self, usuario: UsuarioActual, comando: AgregarTemaComando) -> CompetenciaRespuesta:
        """Agrega el tema, guarda la competencia y publica sus eventos.

        Args:
            usuario: Quien llama; necesita el rol ``ADMINISTRADOR``.
            comando: Competencia y nombre del tema.

        Returns:
            La competencia con el tema nuevo.

        Raises:
            AccesoDenegadoExcepcion: Si falta el rol ``ADMINISTRADOR``.
            SolicitudInvalidaExcepcion: Si el id o el nombre son inválidos.
            CompetenciaNoEncontradaExcepcion: Si la competencia no existe.
            NombreDuplicadoExcepcion: Si ya hay un tema con ese nombre normalizado.
        """
        exigir_administrador(usuario)
        nombre = NombreCatalogo(comando.nombre)
        competencia = cargar_competencia(self._repositorio, comando.competencia_id)
        competencia.agregar_tema(nombre)
        self._repositorio.guardar(competencia)
        self._publicador.publicar(competencia.extraer_eventos())
        return a_competencia_respuesta(competencia)
