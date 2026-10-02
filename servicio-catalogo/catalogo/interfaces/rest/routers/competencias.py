"""Router de ``/api/v1/competencias`` (CONTRATOS.md 8.2).

Los endpoints no tienen reglas de negocio: validan la forma, llaman a un caso de uso y
traducen la respuesta. Las escrituras exigen el rol ``ADMINISTRADOR`` (lo verifica el caso
de uso); las lecturas, cualquier rol. Referencia del Taller 1: D-11 (catálogo administrable).
"""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, Path, Response

from catalogo.aplicacion.casos_uso.agregar_subtema_caso_uso import AgregarSubtemaCasoUso
from catalogo.aplicacion.casos_uso.agregar_tema_caso_uso import AgregarTemaCasoUso
from catalogo.aplicacion.casos_uso.crear_competencia_caso_uso import CrearCompetenciaCasoUso
from catalogo.aplicacion.casos_uso.listar_competencias_caso_uso import ListarCompetenciasCasoUso
from catalogo.aplicacion.casos_uso.obtener_competencia_caso_uso import ObtenerCompetenciaCasoUso
from catalogo.aplicacion.casos_uso.renombrar_competencia_caso_uso import (
    RenombrarCompetenciaCasoUso,
)
from catalogo.aplicacion.casos_uso.renombrar_subtema_caso_uso import RenombrarSubtemaCasoUso
from catalogo.aplicacion.casos_uso.renombrar_tema_caso_uso import RenombrarTemaCasoUso
from catalogo.aplicacion.dtos.comandos import (
    AgregarSubtemaComando,
    AgregarTemaComando,
    CrearCompetenciaComando,
    ObtenerCompetenciaConsulta,
    RenombrarCompetenciaComando,
    RenombrarSubtemaComando,
    RenombrarTemaComando,
)
from catalogo.aplicacion.puertos.salida.ejecutor_transaccional_puerto import (
    EjecutorTransaccionalPuerto,
)
from catalogo.aplicacion.seguridad.usuario_actual import UsuarioActual
from catalogo.dominio.sin_cambio import SIN_CAMBIO
from catalogo.interfaces.rest.dependencias import obtener_ejecutor, obtener_usuario_actual
from catalogo.interfaces.rest.documentacion import respuestas_de_error
from catalogo.interfaces.rest.esquemas import (
    CompetenciaRespuestaModelo,
    CompetenciaSolicitud,
    NombreSolicitud,
)

PREFIJO_API = "/api/v1"

router = APIRouter(prefix=PREFIJO_API + "/competencias", tags=["Competencias"])

Ejecutor = Annotated[EjecutorTransaccionalPuerto, Depends(obtener_ejecutor)]
Usuario = Annotated[UsuarioActual, Depends(obtener_usuario_actual)]

IdCompetencia = Annotated[
    str,
    Path(description="UUID de la competencia (forma canónica con guiones).",
         examples=["22222222-2222-4222-8222-000000000101"]),
]
IdTema = Annotated[
    str,
    Path(description="UUID del tema.", examples=["22222222-2222-4222-8222-000000000201"]),
]
IdSubtema = Annotated[
    str,
    Path(description="UUID del subtema.", examples=["22222222-2222-4222-8222-000000000301"]),
]

ERRORES_COMUNES = ("SOLICITUD_INVALIDA", "NO_AUTENTICADO", "BASE_DE_DATOS_NO_DISPONIBLE", "ERROR_INTERNO")
ERRORES_ESCRITURA = (*ERRORES_COMUNES, "ACCESO_DENEGADO")
NOTA_LOCATION = (
    "El encabezado `Location` apunta a `/api/v1/competencias/{competenciaId}` (la competencia "
    "completa que se responde), porque los temas y subtemas no tienen un `GET` propio. "
)
NOTA_ESCRITURA = "Requiere el rol `ADMINISTRADOR` (la lista de roles debe incluirlo). "
NOTA_NOMBRES = (
    "La unicidad del nombre compara su forma normalizada: sin distinguir mayúsculas, sin "
    "tildes ni diéresis (la `ñ` se conserva como letra propia) y con los espacios sobrantes "
    "quitados. Renombrar con el mismo texto no cambia nada; con una variante (otra mayúscula "
    "o tilde) actualiza el nombre guardado sin contar como duplicado. "
)
CU = "**CU:** administración del catálogo académico (D-11)."


def _url_de_competencia(competencia_id: str) -> str:
    """Ruta del recurso competencia (valor del encabezado ``Location``)."""
    # CONTRATOS 8.2 (v1.11): el Location de los POST de temas y subtemas apunta a la
    # competencia, porque temas y subtemas no tienen un GET propio.
    return f"{PREFIJO_API}/competencias/{competencia_id}"


@router.post(
    "",
    status_code=201,
    response_model=CompetenciaRespuestaModelo,
    summary="Crear una competencia",
    description=NOTA_ESCRITURA + NOTA_NOMBRES + CU,
    responses={
        201: {"description": "Competencia creada. El encabezado `Location` apunta a "
                             "`/api/v1/competencias/{competenciaId}`."},
        **respuestas_de_error(*ERRORES_ESCRITURA, "NOMBRE_DUPLICADO"),
    },
)
async def crear_competencia(
    cuerpo: CompetenciaSolicitud, respuesta: Response, usuario: Usuario, ejecutor: Ejecutor
) -> CompetenciaRespuestaModelo:
    """Crea una competencia y responde 201 con su ``Location``."""
    comando = CrearCompetenciaComando(cuerpo.nombre, cuerpo.descripcion)
    creada = await ejecutor.ejecutar(
        lambda repo, pub: CrearCompetenciaCasoUso(repo, pub).ejecutar(usuario, comando)
    )
    respuesta.headers["Location"] = _url_de_competencia(creada.competencia_id)
    return CompetenciaRespuestaModelo.model_validate(creada)


@router.get(
    "",
    response_model=list[CompetenciaRespuestaModelo],
    summary="Listar el catálogo completo",
    description="Devuelve todas las competencias con sus temas y subtemas anidados. No se "
    "pagina. Competencias, temas y subtemas salen ordenados por nombre normalizado, de forma "
    "ascendente. Cualquier rol puede consultar. " + CU,
    responses=respuestas_de_error(*ERRORES_COMUNES),
)
async def listar_competencias(
    usuario: Usuario, ejecutor: Ejecutor
) -> list[CompetenciaRespuestaModelo]:
    """Lista el catálogo completo."""
    competencias = await ejecutor.ejecutar(
        lambda repo, _pub: ListarCompetenciasCasoUso(repo).ejecutar(usuario)
    )
    return [CompetenciaRespuestaModelo.model_validate(c) for c in competencias]


@router.get(
    "/{competencia_id}",
    response_model=CompetenciaRespuestaModelo,
    summary="Consultar una competencia",
    description="Devuelve una competencia con sus temas y subtemas. Cualquier rol puede "
    "consultar. " + CU,
    responses=respuestas_de_error(*ERRORES_COMUNES, "COMPETENCIA_NO_ENCONTRADA"),
)
async def obtener_competencia(
    competencia_id: IdCompetencia, usuario: Usuario, ejecutor: Ejecutor
) -> CompetenciaRespuestaModelo:
    """Consulta una competencia."""
    consulta = ObtenerCompetenciaConsulta(competencia_id)
    competencia = await ejecutor.ejecutar(
        lambda repo, _pub: ObtenerCompetenciaCasoUso(repo).ejecutar(usuario, consulta)
    )
    return CompetenciaRespuestaModelo.model_validate(competencia)


@router.put(
    "/{competencia_id}",
    response_model=CompetenciaRespuestaModelo,
    summary="Renombrar una competencia",
    description=NOTA_ESCRITURA + NOTA_NOMBRES + "El identificador no cambia (INV-22). "
    "`descripcion`: si no viene se conserva la actual; `null` o `\"\"` la borra; con texto la "
    "reemplaza. " + CU,
    responses=respuestas_de_error(
        *ERRORES_ESCRITURA, "COMPETENCIA_NO_ENCONTRADA", "NOMBRE_DUPLICADO"
    ),
)
async def renombrar_competencia(
    competencia_id: IdCompetencia,
    cuerpo: CompetenciaSolicitud,
    usuario: Usuario,
    ejecutor: Ejecutor,
) -> CompetenciaRespuestaModelo:
    """Renombra una competencia (y opcionalmente cambia su descripción)."""
    # CONTRATOS 8.2 (v1.9): "no viene" (conservar) se distingue de null/"" (borrar).
    descripcion = cuerpo.descripcion if "descripcion" in cuerpo.model_fields_set else SIN_CAMBIO
    comando = RenombrarCompetenciaComando(competencia_id, cuerpo.nombre, descripcion)
    actualizada = await ejecutor.ejecutar(
        lambda repo, pub: RenombrarCompetenciaCasoUso(repo, pub).ejecutar(usuario, comando)
    )
    return CompetenciaRespuestaModelo.model_validate(actualizada)


@router.post(
    "/{competencia_id}/temas",
    status_code=201,
    response_model=CompetenciaRespuestaModelo,
    summary="Agregar un tema a una competencia",
    description=NOTA_ESCRITURA + NOTA_NOMBRES + "El nombre del tema es único dentro de su "
    "competencia (INV-24). Responde la competencia completa. " + NOTA_LOCATION + CU,
    responses={
        201: {"description": "Tema creado. Se responde la competencia completa; `Location` "
                             "apunta a la competencia."},
        **respuestas_de_error(*ERRORES_ESCRITURA, "COMPETENCIA_NO_ENCONTRADA", "NOMBRE_DUPLICADO"),
    },
)
async def agregar_tema(
    competencia_id: IdCompetencia,
    cuerpo: NombreSolicitud,
    respuesta: Response,
    usuario: Usuario,
    ejecutor: Ejecutor,
) -> CompetenciaRespuestaModelo:
    """Agrega un tema y responde 201 con la competencia."""
    comando = AgregarTemaComando(competencia_id, cuerpo.nombre)
    competencia = await ejecutor.ejecutar(
        lambda repo, pub: AgregarTemaCasoUso(repo, pub).ejecutar(usuario, comando)
    )
    respuesta.headers["Location"] = _url_de_competencia(competencia.competencia_id)
    return CompetenciaRespuestaModelo.model_validate(competencia)


@router.put(
    "/{competencia_id}/temas/{tema_id}",
    response_model=CompetenciaRespuestaModelo,
    summary="Renombrar un tema",
    description=NOTA_ESCRITURA + NOTA_NOMBRES + "El identificador no cambia (INV-22). " + CU,
    responses=respuestas_de_error(
        *ERRORES_ESCRITURA,
        "COMPETENCIA_NO_ENCONTRADA",
        "TEMA_NO_ENCONTRADO",
        "NOMBRE_DUPLICADO",
    ),
)
async def renombrar_tema(
    competencia_id: IdCompetencia,
    tema_id: IdTema,
    cuerpo: NombreSolicitud,
    usuario: Usuario,
    ejecutor: Ejecutor,
) -> CompetenciaRespuestaModelo:
    """Renombra un tema."""
    comando = RenombrarTemaComando(competencia_id, tema_id, cuerpo.nombre)
    competencia = await ejecutor.ejecutar(
        lambda repo, pub: RenombrarTemaCasoUso(repo, pub).ejecutar(usuario, comando)
    )
    return CompetenciaRespuestaModelo.model_validate(competencia)


@router.post(
    "/{competencia_id}/temas/{tema_id}/subtemas",
    status_code=201,
    response_model=CompetenciaRespuestaModelo,
    summary="Agregar un subtema a un tema",
    description=NOTA_ESCRITURA + NOTA_NOMBRES + "El nombre del subtema es único dentro de su "
    "tema (INV-24). Responde la competencia completa. " + NOTA_LOCATION + CU,
    responses={
        201: {"description": "Subtema creado. Se responde la competencia completa; `Location` "
                             "apunta a la competencia."},
        **respuestas_de_error(
            *ERRORES_ESCRITURA,
            "COMPETENCIA_NO_ENCONTRADA",
            "TEMA_NO_ENCONTRADO",
            "NOMBRE_DUPLICADO",
        ),
    },
)
async def agregar_subtema(
    competencia_id: IdCompetencia,
    tema_id: IdTema,
    cuerpo: NombreSolicitud,
    respuesta: Response,
    usuario: Usuario,
    ejecutor: Ejecutor,
) -> CompetenciaRespuestaModelo:
    """Agrega un subtema y responde 201 con la competencia."""
    comando = AgregarSubtemaComando(competencia_id, tema_id, cuerpo.nombre)
    competencia = await ejecutor.ejecutar(
        lambda repo, pub: AgregarSubtemaCasoUso(repo, pub).ejecutar(usuario, comando)
    )
    respuesta.headers["Location"] = _url_de_competencia(competencia.competencia_id)
    return CompetenciaRespuestaModelo.model_validate(competencia)


@router.put(
    "/{competencia_id}/temas/{tema_id}/subtemas/{subtema_id}",
    response_model=CompetenciaRespuestaModelo,
    summary="Renombrar un subtema",
    description=NOTA_ESCRITURA + NOTA_NOMBRES + "El identificador no cambia (INV-22). " + CU,
    responses=respuestas_de_error(
        *ERRORES_ESCRITURA,
        "COMPETENCIA_NO_ENCONTRADA",
        "TEMA_NO_ENCONTRADO",
        "SUBTEMA_NO_ENCONTRADO",
        "NOMBRE_DUPLICADO",
    ),
)
async def renombrar_subtema(
    competencia_id: IdCompetencia,
    tema_id: IdTema,
    subtema_id: IdSubtema,
    cuerpo: NombreSolicitud,
    usuario: Usuario,
    ejecutor: Ejecutor,
) -> CompetenciaRespuestaModelo:
    """Renombra un subtema."""
    comando = RenombrarSubtemaComando(competencia_id, tema_id, subtema_id, cuerpo.nombre)
    competencia = await ejecutor.ejecutar(
        lambda repo, pub: RenombrarSubtemaCasoUso(repo, pub).ejecutar(usuario, comando)
    )
    return CompetenciaRespuestaModelo.model_validate(competencia)
