"""Router de ``GET /salud`` (fuera de ``/api/v1``, sin encabezados de identidad)."""

from fastapi import APIRouter

from catalogo.interfaces.rest.esquemas import EstadoSalud

router = APIRouter(tags=["Salud"])

NOMBRE_SERVICIO = "servicio-catalogo"


@router.get(
    "/salud",
    response_model=EstadoSalud,
    summary="Estado del servicio",
    description="Indica que el proceso está vivo. Responde 200 aunque la base de datos no "
    "esté lista (CONTRATOS.md 9.3.6): en ese caso los endpoints que la usan responden 503 "
    "`BASE_DE_DATOS_NO_DISPONIBLE`. No requiere encabezados de identidad.",
)
async def consultar_salud() -> EstadoSalud:
    """Responde ``{"estado": "OK", "servicio": "servicio-catalogo"}``."""
    return EstadoSalud(estado="OK", servicio=NOMBRE_SERVICIO)
