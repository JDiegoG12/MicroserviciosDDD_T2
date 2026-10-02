"""Caso de uso ``SembrarCatalogoCasoUso`` (CONTRATOS.md 4.3 y 11.2)."""

from catalogo.aplicacion.dtos.definiciones_semilla import (
    DefinicionCompetencia,
    SembrarCatalogoComando,
)
from catalogo.aplicacion.dtos.respuestas import SembrarCatalogoResultado
from catalogo.aplicacion.puertos.entrada.puertos_de_entrada import SembrarCatalogoPuertoEntrada
from catalogo.aplicacion.puertos.salida.publicador_eventos_puerto import PublicadorEventosPuerto
from catalogo.dominio.competencia import Competencia
from catalogo.dominio.competencia_repositorio import CompetenciaRepositorio
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.dominio.verificador_nombre_competencia_servicio import (
    VerificadorNombreCompetenciaServicio,
)


class SembrarCatalogoCasoUso(SembrarCatalogoPuertoEntrada):
    """Carga el catálogo semilla con ids fijos, solo si el catálogo está vacío.

    Al arrancar, si no hay competencias, carga la tabla 4.3 de CONTRATOS.md con esos ids
    exactos. Es idempotente: si el catálogo ya tiene datos (de la siembra anterior o creados
    por el administrador) no toca nada. No exige rol: lo ejecuta el propio servicio al arrancar.
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

    def ejecutar(self, comando: SembrarCatalogoComando) -> SembrarCatalogoResultado:
        """Siembra las definiciones si el catálogo está vacío.

        Args:
            comando: Competencias, temas y subtemas con ids fijos.

        Returns:
            Si se sembró y cuántas competencias se crearon.

        Raises:
            SolicitudInvalidaExcepcion: Si una definición tiene un id o un nombre inválidos.
            NombreDuplicadoExcepcion: Si dos definiciones repiten un nombre en su ámbito.
        """
        # CONTRATOS 11.2 (v1.9): la siembra es todo o nada. La atomicidad la da quien ejecuta
        # este caso de uso, dentro de una sola transacción (EjecutorTransaccionalSqlAlchemy).
        if not self._repositorio.esta_vacio():
            return SembrarCatalogoResultado(sembrado=False, competencias_creadas=0)
        for definicion in comando.competencias:
            self._sembrar_competencia(definicion)
        return SembrarCatalogoResultado(
            sembrado=True, competencias_creadas=len(comando.competencias)
        )

    def _sembrar_competencia(self, definicion: DefinicionCompetencia) -> None:
        """Crea una competencia con su jerarquía, la guarda y publica sus eventos."""
        nombre = NombreCatalogo(definicion.nombre)
        self._verificador.exigir_nombre_disponible(nombre)
        competencia = Competencia.crear(
            nombre, definicion.descripcion, CompetenciaId.desde_texto(definicion.id)
        )
        for definicion_tema in definicion.temas:
            tema = competencia.agregar_tema(
                NombreCatalogo(definicion_tema.nombre), TemaId.desde_texto(definicion_tema.id)
            )
            for definicion_subtema in definicion_tema.subtemas:
                competencia.agregar_subtema(
                    tema.id,
                    NombreCatalogo(definicion_subtema.nombre),
                    SubtemaId.desde_texto(definicion_subtema.id),
                )
        self._repositorio.guardar(competencia)
        self._publicador.publicar(competencia.extraer_eventos())
