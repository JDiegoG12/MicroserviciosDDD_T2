"""Adaptador de entrada gRPC: servidor ``grpc.aio`` de ``CatalogoAcademico`` (CONTRATOS.md 6).

* ``catalogo_academico_servicer``: traduce proto → ``ValidarClasificacionCasoUso`` → proto.
* ``servidor_grpc``: arma el servidor con reflexión activada.
* ``generado/``: código generado desde el ``.proto`` por ``scripts/generar_grpc.py``; **no se
  versiona**.
"""
