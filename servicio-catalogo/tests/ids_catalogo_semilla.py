"""Identificadores y nombres de la tabla 4.3 de CONTRATOS.md, escritos a mano.

Son independientes de ``catalogo.aplicacion.datos_semilla`` a propósito: así las pruebas
comprueban que los datos semilla coinciden exactamente con el contrato.
"""

COMPETENCIA_RAZONAMIENTO = "22222222-2222-4222-8222-000000000101"
COMPETENCIA_DISENO = "22222222-2222-4222-8222-000000000102"

TEMA_ESTADISTICA = "22222222-2222-4222-8222-000000000201"
TEMA_ALGEBRA = "22222222-2222-4222-8222-000000000202"
TEMA_PATRONES = "22222222-2222-4222-8222-000000000203"
TEMA_ARQUITECTURA = "22222222-2222-4222-8222-000000000204"

SUBTEMA_TENDENCIA_CENTRAL = "22222222-2222-4222-8222-000000000301"
SUBTEMA_PROBABILIDAD = "22222222-2222-4222-8222-000000000302"
SUBTEMA_ECUACIONES = "22222222-2222-4222-8222-000000000303"
SUBTEMA_CREACIONALES = "22222222-2222-4222-8222-000000000304"
SUBTEMA_MICROSERVICIOS = "22222222-2222-4222-8222-000000000305"

# (competencia, nombre, [(tema, nombre, [(subtema, nombre)])])
TABLA_4_3 = [
    (COMPETENCIA_RAZONAMIENTO, "Razonamiento cuantitativo", [
        (TEMA_ESTADISTICA, "Estadística", [
            (SUBTEMA_TENDENCIA_CENTRAL, "Medidas de tendencia central"),
            (SUBTEMA_PROBABILIDAD, "Probabilidad"),
        ]),
        (TEMA_ALGEBRA, "Álgebra", [
            (SUBTEMA_ECUACIONES, "Ecuaciones lineales"),
        ]),
    ]),
    (COMPETENCIA_DISENO, "Diseño de software", [
        (TEMA_PATRONES, "Patrones de diseño", [
            (SUBTEMA_CREACIONALES, "Patrones creacionales"),
        ]),
        (TEMA_ARQUITECTURA, "Arquitectura de software", [
            (SUBTEMA_MICROSERVICIOS, "Microservicios"),
        ]),
    ]),
]
