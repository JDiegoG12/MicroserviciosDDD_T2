"""Adaptador de entrada REST: API FastAPI de CONTRATOS.md 8.2 bajo ``/api/v1``.

* ``aplicacion_fastapi``: fábrica de la aplicación (Swagger en ``/docs``, OpenAPI en
  ``/openapi.json``).
* ``routers``: endpoints de competencias, temas y subtemas, y ``/salud``.
* ``esquemas``: DTOs JSON en camelCase (propios de esta capa).
* ``dependencias``: arma ``UsuarioActual`` desde ``X-Usuario-Id`` y ``X-Roles``.
* ``errores``: ``application/problem+json`` y el mapa de códigos HTTP (5.3).
* ``middleware_correlacion``: ``X-Id-Correlacion`` y registro de peticiones.

Los routers no llevan reglas de negocio: llaman a un caso de uso.
"""
