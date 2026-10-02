"""Verificación de roles. La verifican los casos de uso; las reglas de propiedad van en el dominio."""

from catalogo.aplicacion.excepciones import AccesoDenegadoExcepcion
from catalogo.aplicacion.seguridad.usuario_actual import Rol, UsuarioActual


def exigir_administrador(usuario: UsuarioActual) -> None:
    """Exige el rol ``ADMINISTRADOR`` para las escrituras del catálogo (D-11, D-08).

    Con varios roles basta con que la lista incluya ``ADMINISTRADOR``.

    Args:
        usuario: Usuario que ejecuta el caso de uso.

    Raises:
        AccesoDenegadoExcepcion: Si el usuario no tiene el rol ``ADMINISTRADOR``.
    """
    if not usuario.tiene_rol(Rol.ADMINISTRADOR):
        raise AccesoDenegadoExcepcion("Solo un ADMINISTRADOR puede modificar el catálogo.")


def exigir_algun_rol(usuario: UsuarioActual) -> None:
    """Exige que el usuario tenga al menos un rol; las lecturas las puede hacer cualquiera.

    Args:
        usuario: Usuario que ejecuta el caso de uso.

    Raises:
        AccesoDenegadoExcepcion: Si el usuario no declara ningún rol.
    """
    # CONTRATOS 4.1 (v1.9): un X-Roles vacío ya se rechaza con 401 en la capa REST; esta
    # comprobación es una defensa por si el caso de uso se llama desde otro adaptador.
    if not usuario.roles:
        raise AccesoDenegadoExcepcion("Se necesita al menos un rol para consultar el catálogo.")
