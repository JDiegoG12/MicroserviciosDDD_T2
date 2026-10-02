# syntax=docker/dockerfile:1
# Dockerfile de servicio-evaluacion (P2). Contexto de construccion: la RAIZ
# del repositorio (CONTRATOS.md 2 y 9.3): se construye con
#   docker compose --profile servicios build servicio-evaluacion
# Imagen base node:24.20.0 (variante slim), version exacta de CONTRATOS.md
# 1.3 y del `engines` de package.json.

# ---------------------------------------------------------------------------
# Etapa 1: construccion (compila TypeScript a JavaScript con nest build)
# ---------------------------------------------------------------------------
FROM node:24.20.0-slim AS construccion
WORKDIR /app

# Capa de dependencias por separado para aprovechar la cache de Docker.
# Aqui se instalan TODAS las dependencias (incluidas las de construccion,
# como el compilador de TypeScript): la etapa de ejecucion nunca ve esta
# capa, solo el resultado compilado en dist/ (CONTRATOS.md 9.3.1).
COPY servicio-evaluacion/package.json servicio-evaluacion/package-lock.json ./
RUN npm ci

# El resto del codigo fuente (node_modules, dist, coverage y test quedan
# fuera por el .dockerignore de la raiz, CONTRATOS.md 2).
COPY servicio-evaluacion/ ./
RUN npm run build

# ---------------------------------------------------------------------------
# Etapa 2: dependencias de produccion, instaladas limpias (sin las de
# construccion/pruebas) a partir del mismo package-lock.json.
# ---------------------------------------------------------------------------
FROM node:24.20.0-slim AS dependencias
WORKDIR /app
COPY servicio-evaluacion/package.json servicio-evaluacion/package-lock.json ./
RUN npm ci --omit=dev

# ---------------------------------------------------------------------------
# Etapa 3: ejecucion (imagen final, sin compiladores ni codigo de pruebas)
# ---------------------------------------------------------------------------
FROM node:24.20.0-slim AS ejecucion

ENV TZ=UTC \
    NODE_ENV=production

# curl es obligatorio: lo usa el HEALTHCHECK de esta imagen y el de
# docker-compose.yml (CONTRATOS.md 9.3.5).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Solo lo estrictamente necesario para correr: nada de src/, test/,
# TypeScript ni dependencias de desarrollo (CONTRATOS.md 9.3.1).
COPY --from=construccion --chown=node:node /app/dist ./dist
COPY --from=dependencias --chown=node:node /app/node_modules ./node_modules
COPY --from=construccion --chown=node:node /app/package.json ./package.json

# La imagen oficial de Node ya trae un usuario sin privilegios llamado
# "node" (CONTRATOS.md 9.3.3): no se crea uno nuevo.
USER node

EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=30s \
    CMD curl -f http://localhost:8083/salud || exit 1

CMD ["node", "dist/main.js"]
