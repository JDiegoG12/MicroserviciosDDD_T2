"""Excepciones de la capa de aplicación, con el código exacto de CONTRATOS.md 5.3 y 8.2."""

from catalogo.dominio.excepciones import CatalogoExcepcion


class CompetenciaNoEncontradaExcepcion(CatalogoExcepcion):
    """La competencia no existe en el catálogo (CONTRATOS.md 5.3, regla 404)."""

    codigo = "COMPETENCIA_NO_ENCONTRADA"


class NoAutenticadoExcepcion(CatalogoExcepcion):
    """Faltan los encabezados de identidad o ``X-Roles`` llega vacío (CONTRATOS.md 4.1, 401)."""

    codigo = "NO_AUTENTICADO"


class BaseDeDatosNoDisponibleExcepcion(CatalogoExcepcion):
    """La base de datos no responde (CONTRATOS.md 5.3, 503)."""

    codigo = "BASE_DE_DATOS_NO_DISPONIBLE"


class AccesoDenegadoExcepcion(CatalogoExcepcion):
    """El rol del usuario no permite la operación (CONTRATOS.md 5.3, regla 403)."""

    codigo = "ACCESO_DENEGADO"
