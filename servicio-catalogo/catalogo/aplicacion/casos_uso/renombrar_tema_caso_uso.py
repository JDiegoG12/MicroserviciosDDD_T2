"""Caso de uso ``RenombrarTemaCasoUso`` (D-11, INV-22, INV-23, INV-24)."""

from catalogo.aplicacion.casos_uso.apoyo_casos_uso import cargar_competencia
from catalogo.aplicacion.dtos.comandos import RenombrarTemaComando
from catalogo.aplicacion.dtos.mapeador_competencia import a_competencia_respuesta
from catalogo.aplicacion.dtos.respuestas import CompetenciaRespuesta
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import RenombrarTemaPuertoEntrada
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.aplicacion.seguridad.autorizacion import exigir_administrador
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo


class RenombrarTemaCasoUso(RenombrarTemaPuertoEntrada):
    """Renombra un tema (``PUT /competencias/{competenciaId}/temas/{temaId}`` → 200).

    Reglas: INV-22 (el identificador no cambia), INV-23 (el tema debe existir en la
    competencia) e INV-24 (nombre único dentro de la competencia; renombrar con el mismo
    nombre o con una variante que se normaliza igual no es duplicado).
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
        self, usuario: UsuarioActual, comando: RenombrarTemaComando
    ) -> CompetenciaRespuesta:
        """Renombra el tema, guarda la competencia y publica sus eventos.

        Args:
            usuario: Quien llama; necesita el rol ``ADMINISTRADOR``.
            comando: Competencia, tema y nombre nuevo.

        Returns:
            La competencia actualizada.

        Raises:
            AccesoDenegadoExcepcion: Si falta el rol ``ADMINISTRADOR``.
            SolicitudInvalidaExcepcion: Si un id o el nombre son inválidos.
            CompetenciaNoEncontradaExcepcion: Si la competencia no existe.
            TemaNoEncontradoExcepcion: Si el tema no está en la competencia.
            NombreDuplicadoExcepcion: Si otro tema tiene ese nombre normalizado.
        """
        exigir_administrador(usuario)
        tema_id = TemaId.desde_texto(comando.tema_id)
        nombre = NombreCatalogo(comando.nombre)
        competencia = cargar_competencia(self._repositorio, comando.competencia_id)
        competencia.renombrar_tema(tema_id, nombre)
        self._repositorio.guardar(competencia)
        self._publicador.publicar(competencia.extraer_eventos())
        return a_competencia_respuesta(competencia)
