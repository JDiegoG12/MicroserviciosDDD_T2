# =============================================================================
# servicio-catalogo · Catálogo Académico (Python 3.12 · FastAPI · gRPC)
#
# El contexto de construcción es la RAÍZ del repositorio (CONTRATOS.md 2 y 9.3):
#   docker compose --profile servicios build servicio-catalogo
# Docker solo lee el .dockerignore de la raíz; no se crea uno en esta carpeta.
#
# Dos etapas:
#   1. construccion: genera el código gRPC desde /contratos/proto (con grpcio-tools) y
#      empaqueta el servicio y sus dependencias como "ruedas" (wheels).
#   2. final: instala solo esas ruedas. No lleva grpcio-tools, pruebas ni compiladores.
# =============================================================================

# ------------------------------------------------------------------ construcción
FROM python:3.12-slim AS construccion

ENV PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# grpcio-tools vive en un entorno aparte: solo sirve para generar el código.
RUN python -m venv /opt/herramientas \
    && /opt/herramientas/bin/pip install grpcio-tools

# Solo lo necesario: el .proto y el código del servicio (sin tests/ ni .venv).
COPY contratos/proto ./contratos/proto
COPY servicio-catalogo/pyproject.toml servicio-catalogo/README.md ./
COPY servicio-catalogo/catalogo ./catalogo
COPY servicio-catalogo/scripts ./scripts

# El código generado nunca se versiona (CONTRATOS.md 6): se genera aquí.
RUN /opt/herramientas/bin/python scripts/generar_grpc.py

# Ruedas del servicio y de sus dependencias de ejecución (psycopg usa su binario: no compila).
RUN rm -rf ./*.egg-info ./build \
    && pip wheel --wheel-dir /ruedas .

# ------------------------------------------------------------------------ final
FROM python:3.12-slim AS final

ENV TZ=UTC \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# curl lo usa el HEALTHCHECK (CONTRATOS.md 9.3.5).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios (CONTRATOS.md 9.3.3).
RUN groupadd --system --gid 10001 catalogo \
    && useradd --system --uid 10001 --gid catalogo --no-create-home --shell /usr/sbin/nologin catalogo

# Se montan las ruedas solo durante la instalación: no quedan en ninguna capa de la imagen.
RUN --mount=type=bind,from=construccion,source=/ruedas,target=/ruedas \
    pip install --no-index --find-links=/ruedas servicio-catalogo \
    && python -c "import catalogo.interfaces.grpc.generado.catalogo_academico_pb2_grpc, catalogo.principal"

USER catalogo
WORKDIR /home/catalogo

# REST (8082) y gRPC (50051), los puertos internos de CONTRATOS.md 9.1.
EXPOSE 8082 50051

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=10 \
    CMD curl -fsS "http://localhost:${CATALOGO_PUERTO_HTTP:-8082}/salud" || exit 1

# Un solo proceso: FastAPI y grpc.aio en el mismo bucle asyncio (CONTRATOS.md 11.2).
CMD ["python", "-m", "catalogo.principal"]
