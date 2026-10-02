"""Agregado ``Competencia`` (raíz) con las entidades internas ``Tema`` y ``Subtema``.

MODELO-DOMINIO.md 7.6 y CONTRATOS.md 11.2. Toda modificación de un tema o de un subtema
pasa por la ``Competencia`` y se guarda como una unidad (un agregado por transacción).
"""

from __future__ import annotations

from catalogo.dominio.eventos import (
    CompetenciaCreada,
    CompetenciaRenombrada,
    EventoDeDominio,
    SubtemaCreado,
    TemaCreado,
)
from catalogo.dominio.excepciones import (
    NombreDuplicadoExcepcion,
    SolicitudInvalidaExcepcion,
    SubtemaNoEncontradoExcepcion,
    TemaNoEncontradoExcepcion,
)
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.dominio.sin_cambio import SIN_CAMBIO, SinCambio
from catalogo.dominio.subtema import Subtema
from catalogo.dominio.tema import Tema

LONGITUD_MAXIMA_DESCRIPCION = 500
"""Máximo de caracteres Unicode de la descripción de una competencia (CONTRATOS.md 4 y 5.3)."""


class Competencia:
    """Raíz del agregado del catálogo académico.

    Invariantes que protege:

    * INV-22: el identificador es estable e independiente del nombre; renombrar no lo cambia.
    * INV-23: no hay huérfanos; todo tema pertenece a esta competencia y todo subtema a un
      tema de ella. Operar sobre un tema o subtema inexistente se rechaza.
    * INV-24: el nombre de un tema es único dentro de su competencia y el de un subtema
      dentro de su tema, comparando la forma normalizada. La unicidad del nombre de la
      *competencia* cruza agregados y la resuelve ``VerificadorNombreCompetenciaServicio``.

    Los eventos de dominio se acumulan en una lista interna y el caso de uso los extrae con
    ``extraer_eventos`` después de guardar.
    """

    def __init__(
        self, id: CompetenciaId, nombre: NombreCatalogo, descripcion: str | None
    ) -> None:
        """Construye la competencia sin eventos. Usar ``crear`` para una competencia nueva.

        Args:
            id: Identificador estable (INV-22).
            nombre: Nombre de la competencia.
            descripcion: Descripción ya validada, o ``None``.
        """
        self._id = id
        self._nombre = nombre
        self._descripcion = descripcion
        self._temas: list[Tema] = []
        self._eventos: list[EventoDeDominio] = []

    @classmethod
    def crear(
        cls,
        nombre: NombreCatalogo,
        descripcion: str | None = None,
        competencia_id: CompetenciaId | None = None,
    ) -> Competencia:
        """Crea una competencia nueva y registra ``CompetenciaCreada`` (D-11).

        Esta operación no comprueba la unicidad del nombre en el catálogo (INV-24): eso
        cruza agregados y lo hace ``VerificadorNombreCompetenciaServicio`` antes de llamar.

        Args:
            nombre: Nombre de la competencia.
            descripcion: Descripción opcional de hasta 500 caracteres.
            competencia_id: Identificador fijo (lo usa la siembra, CONTRATOS.md 4.3); si es
                ``None`` se genera uno nuevo.

        Returns:
            La competencia, sin temas.

        Raises:
            SolicitudInvalidaExcepcion: Si la descripción supera 500 caracteres.
        """
        descripcion_validada = _validar_descripcion(descripcion)
        competencia = cls(competencia_id or CompetenciaId.generar(), nombre, descripcion_validada)
        competencia._eventos.append(CompetenciaCreada(competencia.id, nombre))
        return competencia

    # ---------------------------------------------------------------- consulta

    @property
    def id(self) -> CompetenciaId:
        """Identificador estable de la competencia (INV-22)."""
        return self._id

    @property
    def nombre(self) -> NombreCatalogo:
        """Nombre actual de la competencia."""
        return self._nombre

    @property
    def descripcion(self) -> str | None:
        """Descripción de la competencia, o ``None`` si no tiene (CONTRATOS.md 8.2)."""
        return self._descripcion

    @property
    def temas(self) -> tuple[Tema, ...]:
        """Temas de la competencia, en el orden en que se agregaron (solo lectura)."""
        return tuple(self._temas)

    def buscar_tema(self, tema_id: TemaId) -> Tema | None:
        """Busca un tema de esta competencia.

        Args:
            tema_id: Identificador del tema.

        Returns:
            El tema, o ``None`` si esta competencia no lo contiene.
        """
        return next((t for t in self._temas if t.id == tema_id), None)

    def extraer_eventos(self) -> list[EventoDeDominio]:
        """Entrega los eventos acumulados y vacía la lista interna.

        Returns:
            Los eventos de dominio ocurridos desde la última extracción, en orden.
        """
        eventos = list(self._eventos)
        self._eventos.clear()
        return eventos

    # ----------------------------------------------------------------- comandos

    def renombrar(
        self, nombre_nuevo: NombreCatalogo, descripcion: str | None | SinCambio = SIN_CAMBIO
    ) -> None:
        """Renombra la competencia; el identificador no cambia (INV-22).

        Una variante que se normaliza igual que el nombre actual no es un duplicado. Si el
        texto literal cambia (por ejemplo, para corregir una mayúscula o una tilde) el nombre
        guardado se actualiza y se emite ``CompetenciaRenombrada``; si el texto es idéntico no
        cambia nada ni se emite evento (CONTRATOS.md 11.2, v1.9). La unicidad frente a *otras*
        competencias la verifica ``VerificadorNombreCompetenciaServicio``.

        Args:
            nombre_nuevo: Nombre nuevo.
            descripcion: ``SIN_CAMBIO`` conserva la descripción actual; ``None`` o ``""`` la
                borra; un texto de hasta 500 caracteres la reemplaza (CONTRATOS.md 8.2, v1.9).

        Raises:
            SolicitudInvalidaExcepcion: Si la descripción supera 500 caracteres.
        """
        if descripcion is not SIN_CAMBIO:
            self._descripcion = _validar_descripcion(descripcion)
        if nombre_nuevo == self._nombre:
            return
        nombre_anterior = self._nombre
        self._nombre = nombre_nuevo
        self._eventos.append(CompetenciaRenombrada(self._id, nombre_anterior, nombre_nuevo))

    def agregar_tema(self, nombre: NombreCatalogo, tema_id: TemaId | None = None) -> Tema:
        """Agrega un tema a la competencia y registra ``TemaCreado`` (INV-24).

        Args:
            nombre: Nombre del tema.
            tema_id: Identificador fijo (lo usa la siembra, CONTRATOS.md 4.3); si es ``None``
                se genera uno nuevo.

        Returns:
            El tema creado.

        Raises:
            NombreDuplicadoExcepcion: INV-24, si ya hay un tema con ese nombre normalizado.
            SolicitudInvalidaExcepcion: Si ``tema_id`` ya lo usa un tema de esta competencia.
        """
        tema_id = tema_id or TemaId.generar()
        if self.buscar_tema(tema_id) is not None:
            raise SolicitudInvalidaExcepcion(f"El tema {tema_id} ya existe en la competencia.")
        self._exigir_nombre_de_tema_libre(nombre, excluir=None)
        tema = Tema(tema_id, nombre)
        self._temas.append(tema)
        self._eventos.append(TemaCreado(self._id, tema.id, nombre))
        return tema

    def renombrar_tema(self, tema_id: TemaId, nombre_nuevo: NombreCatalogo) -> None:
        """Renombra un tema; su identificador no cambia (INV-22).

        Una variante que se normaliza igual no es duplicado: si el texto literal cambia, el
        nombre guardado se actualiza; si es idéntico, no cambia nada (CONTRATOS.md 11.2, v1.9).

        Args:
            tema_id: Tema a renombrar.
            nombre_nuevo: Nombre nuevo.

        Raises:
            TemaNoEncontradoExcepcion: INV-23, si el tema no pertenece a esta competencia.
            NombreDuplicadoExcepcion: INV-24, si otro tema de la competencia tiene ese nombre.
        """
        tema = self._obtener_tema(tema_id)
        if tema.nombre == nombre_nuevo:
            return
        self._exigir_nombre_de_tema_libre(nombre_nuevo, excluir=tema)
        tema._renombrar(nombre_nuevo)

    def agregar_subtema(
        self, tema_id: TemaId, nombre: NombreCatalogo, subtema_id: SubtemaId | None = None
    ) -> Subtema:
        """Agrega un subtema a un tema y registra ``SubtemaCreado`` (INV-23, INV-24).

        Args:
            tema_id: Tema al que pertenecerá el subtema.
            nombre: Nombre del subtema.
            subtema_id: Identificador fijo (lo usa la siembra, CONTRATOS.md 4.3); si es
                ``None`` se genera uno nuevo.

        Returns:
            El subtema creado.

        Raises:
            TemaNoEncontradoExcepcion: INV-23, si el tema no existe en esta competencia
                (no pueden existir subtemas huérfanos).
            NombreDuplicadoExcepcion: INV-24, si el tema ya tiene un subtema con ese nombre
                normalizado.
            SolicitudInvalidaExcepcion: Si ``subtema_id`` ya lo usa un subtema de esta competencia.
        """
        tema = self._obtener_tema(tema_id)
        subtema_id = subtema_id or SubtemaId.generar()
        if self._existe_subtema(subtema_id):
            raise SolicitudInvalidaExcepcion(f"El subtema {subtema_id} ya existe en la competencia.")
        subtema = tema._agregar_subtema(subtema_id, nombre)
        self._eventos.append(SubtemaCreado(self._id, tema.id, subtema.id, nombre))
        return subtema

    def renombrar_subtema(
        self, tema_id: TemaId, subtema_id: SubtemaId, nombre_nuevo: NombreCatalogo
    ) -> None:
        """Renombra un subtema; su identificador no cambia (INV-22).

        Args:
            tema_id: Tema al que pertenece el subtema.
            subtema_id: Subtema a renombrar.
            nombre_nuevo: Nombre nuevo.

        Raises:
            TemaNoEncontradoExcepcion: INV-23, si el tema no existe en esta competencia.
            SubtemaNoEncontradoExcepcion: INV-23, si el subtema no pertenece a ese tema.
            NombreDuplicadoExcepcion: INV-24, si otro subtema del tema tiene ese nombre.
        """
        self._obtener_tema(tema_id)._renombrar_subtema(subtema_id, nombre_nuevo)

    # ----------------------------------------------------------------- privados

    def _obtener_tema(self, tema_id: TemaId) -> Tema:
        """Devuelve un tema de esta competencia o rechaza (INV-23).

        Raises:
            TemaNoEncontradoExcepcion: Si el tema no pertenece a esta competencia.
        """
        tema = self.buscar_tema(tema_id)
        if tema is None:
            raise TemaNoEncontradoExcepcion(
                f"El tema {tema_id} no existe en la competencia {self._id}."
            )
        return tema

    def _existe_subtema(self, subtema_id: SubtemaId) -> bool:
        """Indica si algún tema de esta competencia ya usa ese identificador de subtema."""
        return any(t.buscar_subtema(subtema_id) is not None for t in self._temas)

    def _exigir_nombre_de_tema_libre(self, nombre: NombreCatalogo, excluir: Tema | None) -> None:
        """Comprueba INV-24 para los temas: el nombre normalizado no está en uso.

        Args:
            nombre: Nombre que se quiere usar.
            excluir: Tema que se está renombrando, que no cuenta como conflicto.

        Raises:
            NombreDuplicadoExcepcion: Si otro tema de la competencia tiene el mismo nombre
                normalizado.
        """
        for tema in self._temas:
            if tema is not excluir and tema.nombre.normalizado == nombre.normalizado:
                raise NombreDuplicadoExcepcion(
                    f"Ya existe un tema llamado '{tema.nombre}' en la competencia '{self._nombre}'."
                )


def _validar_descripcion(descripcion: str | None) -> str | None:
    """Valida la descripción opcional de una competencia.

    Se cuenta en caracteres Unicode después de quitar los espacios de los extremos
    (CONTRATOS.md 4). Una descripción en blanco equivale a no tener descripción.

    Args:
        descripcion: Texto o ``None``.

    Returns:
        La descripción recortada, o ``None`` si no hay (o si viene en blanco, que equivale
        a borrarla).

    Raises:
        SolicitudInvalidaExcepcion: Si no es texto o supera 500 caracteres.
    """
    if descripcion is None:
        return None
    if not isinstance(descripcion, str):
        raise SolicitudInvalidaExcepcion("La descripción debe ser un texto.")
    recortada = descripcion.strip()
    if len(recortada) > LONGITUD_MAXIMA_DESCRIPCION:
        raise SolicitudInvalidaExcepcion(
            f"La descripción no puede superar {LONGITUD_MAXIMA_DESCRIPCION} caracteres."
        )
    return recortada or None
