"""Entidad ``Tema``, interna del agregado ``Competencia``."""

from __future__ import annotations

from catalogo.dominio.excepciones import NombreDuplicadoExcepcion, SubtemaNoEncontradoExcepcion
from catalogo.dominio.identificadores import SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.dominio.subtema import Subtema


class Tema:
    """Tema de una competencia, que agrupa subtemas.

    Es una entidad interna: no se crea ni se modifica por fuera de ``Competencia``
    (CONTRATOS.md 11.2). Su identidad es ``id`` y no cambia al renombrarlo (INV-22).
    Protege INV-24 para sus subtemas: el nombre de un subtema es único dentro de su tema.
    """

    def __init__(self, id: TemaId, nombre: NombreCatalogo) -> None:
        """Crea un tema sin subtemas.

        Args:
            id: Identificador estable (INV-22).
            nombre: Nombre del tema.
        """
        self._id = id
        self._nombre = nombre
        self._subtemas: list[Subtema] = []

    @property
    def id(self) -> TemaId:
        """Identificador estable del tema (INV-22)."""
        return self._id

    @property
    def nombre(self) -> NombreCatalogo:
        """Nombre actual del tema."""
        return self._nombre

    @property
    def subtemas(self) -> tuple[Subtema, ...]:
        """Subtemas del tema, en el orden en que se agregaron (solo lectura)."""
        return tuple(self._subtemas)

    def buscar_subtema(self, subtema_id: SubtemaId) -> Subtema | None:
        """Busca un subtema de este tema.

        Args:
            subtema_id: Identificador del subtema.

        Returns:
            El subtema, o ``None`` si este tema no lo contiene.
        """
        return next((s for s in self._subtemas if s.id == subtema_id), None)

    def _renombrar(self, nombre_nuevo: NombreCatalogo) -> None:
        """Cambia el nombre. Solo lo invoca ``Competencia``, que valida INV-24.

        Args:
            nombre_nuevo: Nombre nuevo.
        """
        self._nombre = nombre_nuevo

    def _agregar_subtema(self, subtema_id: SubtemaId, nombre: NombreCatalogo) -> Subtema:
        """Agrega un subtema. Solo lo invoca ``Competencia``.

        Args:
            subtema_id: Identificador del subtema nuevo.
            nombre: Nombre del subtema nuevo.

        Returns:
            El subtema creado.

        Raises:
            NombreDuplicadoExcepcion: INV-24, si el nombre normalizado ya existe en este tema.
        """
        self._exigir_nombre_libre(nombre, excluir=None)
        subtema = Subtema(subtema_id, nombre)
        self._subtemas.append(subtema)
        return subtema

    def _renombrar_subtema(self, subtema_id: SubtemaId, nombre_nuevo: NombreCatalogo) -> None:
        """Renombra un subtema. Solo lo invoca ``Competencia``.

        Una variante que se normaliza igual no es duplicado: si el texto literal cambia, el
        nombre guardado se actualiza; si es idéntico, no cambia nada (CONTRATOS.md 11.2, v1.9).

        Args:
            subtema_id: Subtema a renombrar.
            nombre_nuevo: Nombre nuevo.

        Raises:
            SubtemaNoEncontradoExcepcion: INV-23, si el subtema no pertenece a este tema.
            NombreDuplicadoExcepcion: INV-24, si otro subtema de este tema ya tiene ese nombre.
        """
        subtema = self.buscar_subtema(subtema_id)
        if subtema is None:
            raise SubtemaNoEncontradoExcepcion(
                f"El subtema {subtema_id} no existe en el tema {self._id}."
            )
        if subtema.nombre == nombre_nuevo:
            return
        self._exigir_nombre_libre(nombre_nuevo, excluir=subtema)
        subtema._renombrar(nombre_nuevo)

    def _exigir_nombre_libre(self, nombre: NombreCatalogo, excluir: Subtema | None) -> None:
        """Comprueba INV-24 para los subtemas: el nombre normalizado no está en uso.

        Args:
            nombre: Nombre que se quiere usar.
            excluir: Subtema que se está renombrando, que no cuenta como conflicto.

        Raises:
            NombreDuplicadoExcepcion: Si otro subtema del tema tiene el mismo nombre normalizado.
        """
        for subtema in self._subtemas:
            if subtema is not excluir and subtema.nombre.normalizado == nombre.normalizado:
                raise NombreDuplicadoExcepcion(
                    f"Ya existe un subtema llamado '{subtema.nombre}' en el tema '{self._nombre}'."
                )
