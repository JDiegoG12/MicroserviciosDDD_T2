"""Entorno de Alembic.

Alembic se ejecuta desde ``catalogo.infraestructura.inicializacion`` con una conexión que
ya existe (``config.attributes["connection"]``), dentro de ``AsyncConnection.run_sync``. No
abre su propio motor ni toca la configuración del log del servicio.
"""

from alembic import context

from catalogo.infraestructura.base_datos.modelos import Base

config = context.config
conexion = config.attributes.get("connection")

if conexion is None:
    raise RuntimeError(
        "Las migraciones se aplican desde el servicio (inicializacion.aplicar_migraciones)."
    )

context.configure(connection=conexion, target_metadata=Base.metadata)
with context.begin_transaction():
    context.run_migrations()
