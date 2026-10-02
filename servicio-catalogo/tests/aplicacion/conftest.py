"""Fixtures de las pruebas de aplicación."""

import pytest

from catalogo.aplicacion.seguridad.usuario_actual import Rol, UsuarioActual
from tests.dobles.publicador_eventos_en_memoria import PublicadorEventosEnMemoria


@pytest.fixture
def publicador() -> PublicadorEventosEnMemoria:
    """Publicador que recuerda los eventos."""
    return PublicadorEventosEnMemoria()


@pytest.fixture
def administrador() -> UsuarioActual:
    """Administrador de prueba (CONTRATOS.md 4.2)."""
    return UsuarioActual("11111111-1111-4111-8111-000000000001", frozenset({Rol.ADMINISTRADOR}))


@pytest.fixture
def estudiante() -> UsuarioActual:
    """Estudiante de prueba (CONTRATOS.md 4.2)."""
    return UsuarioActual("11111111-1111-4111-8111-000000000006", frozenset({Rol.ESTUDIANTE}))
