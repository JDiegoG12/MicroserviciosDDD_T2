"""Identidad del usuario que ejecuta un caso de uso (CONTRATOS.md 4.1)."""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class Rol(Enum):
    """Roles válidos del sistema (CONTRATOS.md 4.1, RF-03)."""

    ADMINISTRADOR = "ADMINISTRADOR"
    AUTOR = "AUTOR"
    REVISOR = "REVISOR"
    DOCENTE = "DOCENTE"
    ESTUDIANTE = "ESTUDIANTE"


@dataclass(frozen=True, slots=True)
class UsuarioActual:
    """Usuario que llama al caso de uso.

    La etapa 2 lo arma desde los encabezados ``X-Usuario-Id`` y ``X-Roles``. Un usuario
    puede tener varios roles a la vez (D-08).

    Attributes:
        usuario_id: Identificador del usuario (UUID en texto).
        roles: Roles que declara el usuario.
    """

    usuario_id: str
    roles: frozenset[Rol]

    def tiene_rol(self, rol: Rol) -> bool:
        """Indica si la lista de roles del usuario incluye el rol dado (D-08).

        Args:
            rol: Rol que se busca.

        Returns:
            ``True`` si el usuario tiene ese rol, aunque tenga otros.
        """
        return rol in self.roles
