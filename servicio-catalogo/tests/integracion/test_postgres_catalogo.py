"""Integración con PostgreSQL real: migraciones, repositorio, índices únicos y siembra."""

from __future__ import annotations

import pytest
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncEngine, async_sessionmaker

from catalogo.aplicacion.excepciones import BaseDeDatosNoDisponibleExcepcion
from catalogo.dominio.competencia import Competencia
from catalogo.dominio.excepciones import NombreDuplicadoExcepcion
from catalogo.dominio.identificadores import CompetenciaId, SubtemaId, TemaId
from catalogo.dominio.nombre_catalogo import NombreCatalogo
from catalogo.infraestructura.base_datos.ejecutor_transaccional_sqlalchemy import (
    EjecutorTransaccionalSqlAlchemy,
    EstadoInicializacion,
    crear_motor,
)
from catalogo.infraestructura.inicializacion import (
    aplicar_migraciones,
    inicializar_base_de_datos,
    sembrar_catalogo,
)
from tests import ids_catalogo_semilla as ids
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria


def n(texto: str) -> NombreCatalogo:
    return NombreCatalogo(texto)


def competencia_con_jerarquia() -> Competencia:
    competencia = Competencia.crear(n("Comunicación escrita"), "Lectura y escritura")
    tema = competencia.agregar_tema(n("Año académico"))
    competencia.agregar_subtema(tema.id, n("Ensayo"))
    competencia.agregar_subtema(tema.id, n("Resumen"))
    return competencia


async def guardar(ejecutor: EjecutorTransaccionalSqlAlchemy, competencia: Competencia) -> None:
    await ejecutor.ejecutar(lambda repositorio, _pub: repositorio.guardar(competencia))


async def escalar(motor: AsyncEngine, sql: str):
    async with motor.connect() as conexion:
        return (await conexion.execute(text(sql))).scalar_one()


# ------------------------------------------------------------------ migraciones


async def test_la_migracion_crea_las_tres_tablas_con_columna_uuid(motor_migrado: AsyncEngine) -> None:
    tablas = await escalar(
        motor_migrado,
        "SELECT string_agg(table_name, ',' ORDER BY table_name) FROM information_schema.tables "
        "WHERE table_schema = 'public'",
    )
    assert tablas == "alembic_version,competencias,subtemas,temas"
    tipo = await escalar(
        motor_migrado,
        "SELECT data_type FROM information_schema.columns "
        "WHERE table_name = 'competencias' AND column_name = 'id'",
    )
    assert tipo == "uuid"


async def test_aplicar_las_migraciones_dos_veces_es_idempotente(motor_migrado: AsyncEngine) -> None:
    await aplicar_migraciones(motor_migrado)
    assert await escalar(motor_migrado, "SELECT version_num FROM alembic_version") == "0001"


async def test_la_migracion_no_siembra_datos(motor_migrado: AsyncEngine) -> None:
    assert await escalar(motor_migrado, "SELECT count(*) FROM competencias") == 0


# ------------------------------------------------------------------ repositorio


async def test_guarda_y_carga_el_agregado_completo(ejecutor: EjecutorTransaccionalSqlAlchemy) -> None:
    competencia = competencia_con_jerarquia()
    await guardar(ejecutor, competencia)

    cargada = await ejecutor.ejecutar(lambda repo, _pub: repo.obtener_por_id(competencia.id))

    assert cargada is not None
    assert cargada.id == competencia.id
    assert cargada.nombre.valor == "Comunicación escrita"
    assert cargada.descripcion == "Lectura y escritura"
    (tema,) = cargada.temas
    assert tema.nombre.valor == "Año académico"
    assert [s.nombre.valor for s in tema.subtemas] == ["Ensayo", "Resumen"]
    assert cargada.extraer_eventos() == []


async def test_obtener_por_id_de_una_competencia_inexistente_devuelve_none(
    ejecutor: EjecutorTransaccionalSqlAlchemy,
) -> None:
    resultado = await ejecutor.ejecutar(lambda repo, _pub: repo.obtener_por_id(CompetenciaId.generar()))
    assert resultado is None


async def test_actualizar_conserva_los_ids_y_persiste_renombres_y_elementos_nuevos(
    ejecutor: EjecutorTransaccionalSqlAlchemy,
) -> None:
    competencia = competencia_con_jerarquia()
    await guardar(ejecutor, competencia)
    tema_id = competencia.temas[0].id

    def modificar(repositorio, _publicador):
        cargada = repositorio.obtener_por_id(competencia.id)
        cargada.renombrar(n("Comunicación oral"), None)
        cargada.renombrar_tema(tema_id, n("Año escolar"))
        cargada.agregar_subtema(tema_id, n("Informe"))
        cargada.agregar_tema(n("Otro tema"))
        repositorio.guardar(cargada)

    await ejecutor.ejecutar(modificar)

    final = await ejecutor.ejecutar(lambda repo, _pub: repo.obtener_por_id(competencia.id))
    assert final.id == competencia.id
    assert final.nombre.valor == "Comunicación oral"
    assert final.descripcion is None
    assert {t.nombre.valor for t in final.temas} == {"Año escolar", "Otro tema"}
    assert final.buscar_tema(tema_id) is not None
    assert len(final.buscar_tema(tema_id).subtemas) == 3


async def test_listar_todas_y_esta_vacio(ejecutor: EjecutorTransaccionalSqlAlchemy) -> None:
    assert await ejecutor.ejecutar(lambda repo, _pub: repo.esta_vacio()) is True
    await guardar(ejecutor, Competencia.crear(n("Zeta")))
    await guardar(ejecutor, Competencia.crear(n("Álgebra")))
    assert await ejecutor.ejecutar(lambda repo, _pub: repo.esta_vacio()) is False
    nombres = await ejecutor.ejecutar(lambda repo, _pub: [c.nombre.valor for c in repo.listar_todas()])
    assert nombres == ["Álgebra", "Zeta"]


async def test_consultas_de_existencia(ejecutor: EjecutorTransaccionalSqlAlchemy) -> None:
    competencia = competencia_con_jerarquia()
    await guardar(ejecutor, competencia)
    tema = competencia.temas[0]
    subtema = tema.subtemas[0]

    def consultar(repo, _pub):
        return (
            repo.existe_competencia_con_nombre(n("COMUNICACIÓN escrita").normalizado),
            repo.existe_competencia_con_nombre(n("otra cosa").normalizado),
            repo.existe_competencia_con_nombre(n("Comunicación escrita").normalizado, competencia.id),
            repo.existe_tema(tema.id),
            repo.existe_tema(TemaId.generar()),
            repo.existe_subtema(subtema.id),
            repo.existe_subtema(SubtemaId.generar()),
        )

    assert await ejecutor.ejecutar(consultar) == (True, False, False, True, False, True, False)


# ------------------------------------------------------- INV-23 e INV-24 en la base


async def test_inv23_la_llave_foranea_impide_temas_y_subtemas_huerfanos(
    motor_migrado: AsyncEngine,
) -> None:
    async with motor_migrado.connect() as conexion:
        with pytest.raises(IntegrityError):
            await conexion.execute(
                text(
                    "INSERT INTO temas (id, competencia_id, nombre, nombre_normalizado) "
                    "VALUES (gen_random_uuid(), gen_random_uuid(), 'Huérfano', 'huerfano')"
                )
            )
    async with motor_migrado.connect() as conexion:
        with pytest.raises(IntegrityError):
            await conexion.execute(
                text(
                    "INSERT INTO subtemas (id, tema_id, nombre, nombre_normalizado) "
                    "VALUES (gen_random_uuid(), gen_random_uuid(), 'Huérfano', 'huerfano')"
                )
            )


async def test_inv24_el_indice_unico_rechaza_competencias_con_el_mismo_nombre_normalizado(
    ejecutor: EjecutorTransaccionalSqlAlchemy,
) -> None:
    await guardar(ejecutor, Competencia.crear(n("Diseño de software")))
    with pytest.raises(NombreDuplicadoExcepcion) as error:
        await guardar(ejecutor, Competencia.crear(n("  DISEÑO  de software ")))
    assert error.value.codigo == "NOMBRE_DUPLICADO"


async def test_inv24_el_indice_unico_rechaza_temas_repetidos_en_la_misma_competencia(
    ejecutor: EjecutorTransaccionalSqlAlchemy,
) -> None:
    competencia = Competencia.crear(n("A"))
    tema = competencia.agregar_tema(n("Estadística"))
    await guardar(ejecutor, competencia)

    def insertar_tema_duplicado(repositorio, _publicador):
        # Se evita el dominio a propósito: se prueba la defensa de la base de datos.
        repositorio._sesion.execute(
            text(
                "INSERT INTO temas (id, competencia_id, nombre, nombre_normalizado) "
                "VALUES (gen_random_uuid(), :competencia, 'ESTADISTICA', 'estadistica')"
            ),
            {"competencia": competencia.id.valor},
        )

    with pytest.raises(NombreDuplicadoExcepcion):
        await ejecutor.ejecutar(insertar_tema_duplicado)
    assert tema is not None


async def test_inv24_el_indice_unico_rechaza_subtemas_repetidos_en_el_mismo_tema(
    ejecutor: EjecutorTransaccionalSqlAlchemy,
) -> None:
    competencia = competencia_con_jerarquia()
    await guardar(ejecutor, competencia)
    tema_id = competencia.temas[0].id.valor

    def insertar_subtema_duplicado(repositorio, _publicador):
        repositorio._sesion.execute(
            text(
                "INSERT INTO subtemas (id, tema_id, nombre, nombre_normalizado) "
                "VALUES (gen_random_uuid(), :tema, 'ENSAYO', 'ensayo')"
            ),
            {"tema": tema_id},
        )

    with pytest.raises(NombreDuplicadoExcepcion):
        await ejecutor.ejecutar(insertar_subtema_duplicado)


async def test_el_mismo_nombre_de_tema_en_otra_competencia_si_se_permite(
    ejecutor: EjecutorTransaccionalSqlAlchemy,
) -> None:
    for nombre_competencia in ("Una", "Otra"):
        competencia = Competencia.crear(n(nombre_competencia))
        competencia.agregar_tema(n("Estadística"))
        await guardar(ejecutor, competencia)


async def test_la_enie_se_conserva_en_el_nombre_normalizado_guardado(
    ejecutor: EjecutorTransaccionalSqlAlchemy, motor_migrado: AsyncEngine
) -> None:
    await guardar(ejecutor, Competencia.crear(n("Año")))
    await guardar(ejecutor, Competencia.crear(n("Ano")))  # distinta de "Año" (CONTRATOS 11.2 v1.9)
    assert await escalar(motor_migrado, "SELECT count(*) FROM competencias") == 2
    assert await escalar(
        motor_migrado, "SELECT nombre_normalizado FROM competencias WHERE nombre = 'Año'"
    ) == "año"


# ----------------------------------------------------------------------- ejecutor


async def test_si_el_caso_de_uso_falla_se_revierte_toda_la_transaccion(
    ejecutor: EjecutorTransaccionalSqlAlchemy, motor_migrado: AsyncEngine
) -> None:
    def guardar_y_fallar(repositorio, _publicador):
        repositorio.guardar(competencia_con_jerarquia())
        raise RuntimeError("falla a mitad de camino")

    with pytest.raises(RuntimeError):
        await ejecutor.ejecutar(guardar_y_fallar)
    assert await escalar(motor_migrado, "SELECT count(*) FROM competencias") == 0
    assert await escalar(motor_migrado, "SELECT count(*) FROM subtemas") == 0


async def test_sin_base_de_datos_lanza_base_de_datos_no_disponible() -> None:
    motor_caido = crear_motor("postgresql+psycopg://catalogo:catalogo@127.0.0.1:1/catalogo")
    ejecutor_caido = EjecutorTransaccionalSqlAlchemy(
        async_sessionmaker(motor_caido), PublicadorEventosEnMemoria(), EstadoInicializacion(catalogo_listo=True)
    )
    try:
        with pytest.raises(BaseDeDatosNoDisponibleExcepcion) as error:
            await ejecutor_caido.ejecutar(lambda repo, _pub: repo.esta_vacio())
        assert error.value.codigo == "BASE_DE_DATOS_NO_DISPONIBLE"
    finally:
        await motor_caido.dispose()


async def test_mientras_la_base_no_este_lista_responde_no_disponible(motor_migrado: AsyncEngine) -> None:
    ejecutor_no_listo = EjecutorTransaccionalSqlAlchemy(
        async_sessionmaker(motor_migrado), PublicadorEventosEnMemoria(), EstadoInicializacion(catalogo_listo=False)
    )
    with pytest.raises(BaseDeDatosNoDisponibleExcepcion):
        await ejecutor_no_listo.ejecutar(lambda repo, _pub: repo.esta_vacio())


# ------------------------------------------------------------------------- siembra


async def contar(motor: AsyncEngine) -> tuple[int, int, int]:
    return (
        await escalar(motor, "SELECT count(*) FROM competencias"),
        await escalar(motor, "SELECT count(*) FROM temas"),
        await escalar(motor, "SELECT count(*) FROM subtemas"),
    )


async def test_la_siembra_carga_exactamente_la_tabla_4_3(
    ejecutor: EjecutorTransaccionalSqlAlchemy, motor_migrado: AsyncEngine
) -> None:
    assert await sembrar_catalogo(ejecutor) is True
    assert await contar(motor_migrado) == (2, 4, 5)

    esperado = sorted(
        [(c_id, c_nombre) for c_id, c_nombre, _ in ids.TABLA_4_3]
        + [(t_id, t_nombre) for _, _, temas in ids.TABLA_4_3 for t_id, t_nombre, _ in temas]
        + [
            (s_id, s_nombre)
            for _, _, temas in ids.TABLA_4_3
            for _, _, subtemas in temas
            for s_id, s_nombre in subtemas
        ]
    )
    async with motor_migrado.connect() as conexion:
        filas = (
            await conexion.execute(
                text(
                    "SELECT id::text, nombre FROM competencias UNION ALL "
                    "SELECT id::text, nombre FROM temas UNION ALL "
                    "SELECT id::text, nombre FROM subtemas"
                )
            )
        ).all()
    assert sorted(tuple(f) for f in filas) == esperado


async def test_una_segunda_siembra_no_duplica_nada(
    ejecutor: EjecutorTransaccionalSqlAlchemy, motor_migrado: AsyncEngine
) -> None:
    await sembrar_catalogo(ejecutor)
    assert await sembrar_catalogo(ejecutor) is False
    assert await contar(motor_migrado) == (2, 4, 5)


async def test_reiniciar_el_servicio_no_duplica_la_siembra(
    motor: AsyncEngine, publicador: PublicadorEventosEnMemoria
) -> None:
    estado = EstadoInicializacion()
    ejecutor_nuevo = EjecutorTransaccionalSqlAlchemy(
        async_sessionmaker(motor, expire_on_commit=False), publicador, estado
    )
    await inicializar_base_de_datos(motor, ejecutor_nuevo, estado)
    assert estado.catalogo_listo is True
    await inicializar_base_de_datos(motor, ejecutor_nuevo, EstadoInicializacion())  # "reinicio"
    assert await contar(motor) == (2, 4, 5)


async def test_la_siembra_es_todo_o_nada(
    ejecutor: EjecutorTransaccionalSqlAlchemy, motor_migrado: AsyncEngine
) -> None:
    from catalogo.aplicacion.casos_uso.sembrar_catalogo_caso_uso import SembrarCatalogoCasoUso
    from catalogo.aplicacion.datos_semilla import CATALOGO_SEMILLA
    from catalogo.aplicacion.dtos.definiciones_semilla import (
        DefinicionCompetencia,
        SembrarCatalogoComando,
    )

    # La segunda competencia repite el nombre de la primera: la siembra falla a la mitad.
    defectuoso = SembrarCatalogoComando(
        (
            CATALOGO_SEMILLA[0],
            DefinicionCompetencia("22222222-2222-4222-8222-000000000999", "RAZONAMIENTO cuantitativo"),
        )
    )
    with pytest.raises(NombreDuplicadoExcepcion):
        await ejecutor.ejecutar(
            lambda repo, pub: SembrarCatalogoCasoUso(repo, pub).ejecutar(defectuoso),
        )
    assert await contar(motor_migrado) == (0, 0, 0)
