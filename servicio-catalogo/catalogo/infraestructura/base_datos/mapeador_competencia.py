"""Mapeador entre el agregado ``Competencia`` y sus modelos SQLAlchemy."""

from __future__ import annotations

from catalogo.dominio.competencia import Competencia
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.infraestructura.base_datos.modelos import (
    CompetenciaModelo,
    SubtemaModelo,
    TemaModelo,
)


def a_dominio(fila: CompetenciaModelo) -> Competencia:
    """Reconstruye el agregado desde sus filas, sin eventos pendientes.

    Se usa la API pública del agregado (``crear``, ``agregar_tema``, ``agregar_subtema``), así
    que los datos guardados vuelven a pasar por las invariantes del dominio. Los eventos que
    esa reconstrucción genera se descartan: describen un hecho pasado que ya ocurrió.

    Args:
        fila: Fila de la competencia con sus temas y subtemas cargados.

    Returns:
        La competencia con su jerarquía completa.
    """
    competencia = Competencia.crear(
        NombreCatalogo(fila.nombre), fila.descripcion, CompetenciaId(fila.id)
    )
    for fila_tema in fila.temas:
        tema = competencia.agregar_tema(NombreCatalogo(fila_tema.nombre), TemaId(fila_tema.id))
        for fila_subtema in fila_tema.subtemas:
            competencia.agregar_subtema(
                tema.id, NombreCatalogo(fila_subtema.nombre), SubtemaId(fila_subtema.id)
            )
    competencia.extraer_eventos()
    return competencia


def aplicar_a_fila(competencia: Competencia, fila: CompetenciaModelo) -> None:
    """Copia el estado del agregado a su fila, creando las filas nuevas de temas y subtemas.

    Nunca elimina filas: el catálogo no tiene ``DELETE`` (D-13). Una fila existente se
    actualiza por su identificador; una que no existe se agrega.

    Args:
        competencia: Agregado con el estado a guardar.
        fila: Fila de la competencia (nueva o cargada con su jerarquía).
    """
    fila.nombre = competencia.nombre.valor
    fila.nombre_normalizado = competencia.nombre.normalizado
    fila.descripcion = competencia.descripcion
    temas_existentes = {t.id: t for t in fila.temas}
    for tema in competencia.temas:
        fila_tema = temas_existentes.get(tema.id.valor)
        if fila_tema is None:
            fila_tema = TemaModelo(id=tema.id.valor, competencia_id=fila.id)
            fila.temas.append(fila_tema)
        fila_tema.nombre = tema.nombre.valor
        fila_tema.nombre_normalizado = tema.nombre.normalizado
        subtemas_existentes = {s.id: s for s in fila_tema.subtemas}
        for subtema in tema.subtemas:
            fila_subtema = subtemas_existentes.get(subtema.id.valor)
            if fila_subtema is None:
                fila_subtema = SubtemaModelo(id=subtema.id.valor, tema_id=tema.id.valor)
                fila_tema.subtemas.append(fila_subtema)
            fila_subtema.nombre = subtema.nombre.valor
            fila_subtema.nombre_normalizado = subtema.nombre.normalizado
