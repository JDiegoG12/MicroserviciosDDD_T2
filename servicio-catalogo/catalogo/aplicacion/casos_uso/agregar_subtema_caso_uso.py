"""Caso de uso ``AgregarSubtemaCasoUso`` (D-11, INV-23, INV-24)."""

from catalogo.aplicacion.casos_uso.apoyo_casos_uso import cargar_competencia
from catalogo.aplicacion.dtos.comandos import AgregarSubtemaComando
from catalogo.aplicacion.dtos.mapeador_competencia import a_competencia_respuesta
from catalogo.aplicacion.dtos.respuestas import CompetenciaRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import AgregarSubtemaPuertoEntrada
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.aplicacion.seguridad.autorizacion import exigir_administrador
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo


class AgregarSubtemaCasoUso(AgregarSubtemaPuertoEntrada):
    """Agrega un subtema a un tema (``POST .../temas/{temaId}/subtemas`` → 201).

    Reglas: INV-23 (el subtema nace dentro de un tema existente; no hay huérfanos) e
    INV-24 (nombre de subtema único dentro de su tema).
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

    def ejecutar(
        self, usuario: UsuarioActual, comando: AgregarSubtemaComando
    ) -> CompetenciaRespuesta:
        """Agrega el subtema, guarda la competencia y publica sus eventos.

        Args:
            usuario: Quien llama; necesita el rol ``ADMINISTRADOR``.
            comando: Competencia, tema y nombre del subtema.

        Returns:
            La competencia con el subtema nuevo.

        Raises:
            AccesoDenegadoExcepcion: Si falta el rol ``ADMINISTRADOR``.
            SolicitudInvalidaExcepcion: Si un id o el nombre son inválidos.
            CompetenciaNoEncontradaExcepcion: Si la competencia no existe.
            TemaNoEncontradoExcepcion: Si el tema no está en la competencia.
            NombreDuplicadoExcepcion: Si el tema ya tiene un subtema con ese nombre normalizado.
        """
        exigir_administrador(usuario)
        tema_id = TemaId.desde_texto(comando.tema_id)
        nombre = NombreCatalogo(comando.nombre)
        competencia = cargar_competencia(self._repositorio, comando.competencia_id)
        competencia.agregar_subtema(tema_id, nombre)
        self._repositorio.guardar(competencia)
        self._publicador.publicar(competencia.extraer_eventos())
        return a_competencia_respuesta(competencia)
