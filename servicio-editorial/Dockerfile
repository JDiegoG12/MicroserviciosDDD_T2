# =============================================================================
# servicio-editorial · Dockerfile multietapa (CONTRATOS.md 9.3)
# El contexto de construcción es la RAÍZ del repositorio (CONTRATOS.md 2):
#   docker compose --profile servicios build servicio-editorial
# Se respeta la misma ruta relativa que en el equipo (servicio-editorial/../contratos/proto), así el pom.xml
# genera el código gRPC igual dentro y fuera de Docker.
# =============================================================================

# --- Etapa 1: construcción (Maven + JDK). No llega a la imagen final. ---------
FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /app

# El .proto vive fuera del servicio y se usa sin copias editadas (CONTRATOS.md 6 y 12).
COPY contratos/proto ./contratos/proto

# Primero solo el pom.xml, para aprovechar la caché de dependencias entre construcciones.
COPY servicio-editorial/pom.xml ./servicio-editorial/pom.xml
WORKDIR /app/servicio-editorial
RUN mvn -B -q dependency:go-offline

# Luego el código. Las pruebas se ejecutan fuera de la imagen (necesitan Docker para Testcontainers).
COPY servicio-editorial/src ./src
RUN mvn -B -q -DskipTests package

# --- Etapa 2: ejecución (solo JRE + curl para el healthcheck) ----------------
FROM eclipse-temurin:21-jre

ENV TZ=UTC

# curl lo usa el HEALTHCHECK (9.3.5). Usuario sin privilegios (9.3.3).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system editorial \
    && useradd --system --gid editorial --no-create-home --shell /usr/sbin/nologin editorial

WORKDIR /app
COPY --from=construccion /app/servicio-editorial/target/servicio-editorial.jar /app/servicio-editorial.jar

USER editorial

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD curl -f http://localhost:8081/salud || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/servicio-editorial.jar"]
