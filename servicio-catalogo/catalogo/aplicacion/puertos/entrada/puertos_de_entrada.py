"""Puertos de entrada: el contrato de cada caso de uso, que usarán los adaptadores de la etapa 2.

Los controladores REST y el servicer gRPC dependen de estas interfaces y no de las clases
concretas. Las escrituras exigen el rol ``ADMINISTRADOR``; las lecturas, cualquier rol.
"""

from abc import ABC, abstractmethod

from catalogo.aplicacion.dtos.comandos import (
    AgregarSubtemaComando,
    AgregarTemaComando,
    CrearCompetenciaComando,
    ObtenerCompetenciaConsulta,
    RenombrarCompetenciaComando,
    RenombrarSubtemaComando,
    RenombrarTemaComando,
    ValidarClasificacionConsulta,
)
from catalogo.aplicacion.dtos.definiciones_semilla import SembrarCatalogoComando
from catalogo.aplicacion.dtos.respuestas import (
    CompetenciaRespuesta,
    SembrarCatalogoResultado,
    ValidacionClasificacionRespuesta,
)
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual


class CrearCompetenciaPuertoEntrada(ABC):
    """Crea una competencia (``POST /competencias``)."""

    @abstractmethod
    def ejecutar(
        self, usuario: UsuarioActual, comando: CrearCompetenciaComando
    ) -> CompetenciaRespuesta:
        """Crea una competencia nueva.

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


class RenombrarCompetenciaPuertoEntrada(ABC):
    """Renombra una competencia (``PUT /competencias/{competenciaId}``)."""

    @abstractmethod
    def ejecutar(
        self, usuario: UsuarioActual, comando: RenombrarCompetenciaComando
    ) -> CompetenciaRespuesta:
        """Renombra una competencia.

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


class AgregarTemaPuertoEntrada(ABC):
    """Agrega un tema a una competencia (``POST /competencias/{competenciaId}/temas``)."""

    @abstractmethod
    def ejecutar(self, usuario: UsuarioActual, comando: AgregarTemaComando) -> CompetenciaRespuesta:
        """Agrega un tema.

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


class RenombrarTemaPuertoEntrada(ABC):
    """Renombra un tema (``PUT /competencias/{competenciaId}/temas/{temaId}``)."""

    @abstractmethod
    def ejecutar(
        self, usuario: UsuarioActual, comando: RenombrarTemaComando
    ) -> CompetenciaRespuesta:
        """Renombra un tema.

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


class AgregarSubtemaPuertoEntrada(ABC):
    """Agrega un subtema a un tema (``POST .../temas/{temaId}/subtemas``)."""

    @abstractmethod
    def ejecutar(
        self, usuario: UsuarioActual, comando: AgregarSubtemaComando
    ) -> CompetenciaRespuesta:
        """Agrega un subtema.

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


class RenombrarSubtemaPuertoEntrada(ABC):
    """Renombra un subtema (``PUT .../temas/{temaId}/subtemas/{subtemaId}``)."""

    @abstractmethod
    def ejecutar(
        self, usuario: UsuarioActual, comando: RenombrarSubtemaComando
    ) -> CompetenciaRespuesta:
        """Renombra un subtema.

        Args:
            usuario: Quien llama; necesita el rol ``ADMINISTRADOR``.
            comando: Competencia, tema, subtema y nombre nuevo.

        Returns:
            La competencia actualizada.

        Raises:
            AccesoDenegadoExcepcion: Si falta el rol ``ADMINISTRADOR``.
            SolicitudInvalidaExcepcion: Si un id o el nombre son inválidos.
            CompetenciaNoEncontradaExcepcion: Si la competencia no existe.
            TemaNoEncontradoExcepcion: Si el tema no está en la competencia.
            SubtemaNoEncontradoExcepcion: Si el subtema no está en el tema.
            NombreDuplicadoExcepcion: Si otro subtema del tema tiene ese nombre normalizado.
        """


class ListarCompetenciasPuertoEntrada(ABC):
    """Lista el catálogo completo (``GET /competencias``)."""

    @abstractmethod
    def ejecutar(self, usuario: UsuarioActual) -> list[CompetenciaRespuesta]:
        """Lista todas las competencias, sin paginar, ordenadas por nombre normalizado.

        Args:
            usuario: Quien llama; vale cualquier rol.

        Returns:
            Las competencias con sus temas y subtemas anidados.

        Raises:
            AccesoDenegadoExcepcion: Si el usuario no declara ningún rol.
        """


class ObtenerCompetenciaPuertoEntrada(ABC):
    """Consulta una competencia (``GET /competencias/{competenciaId}``)."""

    @abstractmethod
    def ejecutar(
        self, usuario: UsuarioActual, consulta: ObtenerCompetenciaConsulta
    ) -> CompetenciaRespuesta:
        """Obtiene una competencia con su jerarquía.

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


class ValidarClasificacionPuertoEntrada(ABC):
    """Valida una terna Competencia/Tema/Subtema (gRPC ``ValidarClasificacion``, INV-07)."""

    @abstractmethod
    def ejecutar(self, consulta: ValidarClasificacionConsulta) -> ValidacionClasificacionRespuesta:
        """Valida la terna. No exige rol: es una llamada interna entre servicios.

        Args:
            consulta: Los tres identificadores, en texto.

        Returns:
            El resultado; una terna inválida no es un error sino ``valida=False`` con motivo.

        Raises:
            SolicitudInvalidaExcepcion: Si algún id no es un UUID (en gRPC, ``INVALID_ARGUMENT``).
        """


class SembrarCatalogoPuertoEntrada(ABC):
    """Carga el catálogo semilla al arrancar (CONTRATOS.md 4.3 y 11.2)."""

    @abstractmethod
    def ejecutar(self, comando: SembrarCatalogoComando) -> SembrarCatalogoResultado:
        """Siembra el catálogo solo si está vacío; es idempotente. No exige rol (arranque).

        Args:
            comando: Definiciones con ids fijos.

        Returns:
            Si se sembró y cuántas competencias se crearon.
        """
