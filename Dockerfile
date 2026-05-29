# syntax=docker/dockerfile:1
#
# DHBW Memory — multi-stage Docker build.
#
# Stage 1 builds the production jar (Vaadin frontend bundled by Vite +
# Spring Boot fat jar). Stage 2 ships only the JRE + the jar, keeping the
# runtime image small and free of build tooling.

# ── Build stage ──────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the build descriptors first; this layer is cached between code-only
# changes so dependencies don't have to be re-resolved on every rebuild.
COPY pom.xml ./
COPY package.json vite.config.ts ./

COPY src ./src

# --mount=type=cache keeps the local Maven repository between builds so
# dependencies are not re-downloaded on every code change.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package -P production

# ── Runtime stage ────────────────────────────────────────────────────────
FROM eclipse-temurin:21.0.7_6-jre-alpine
WORKDIR /app

# Non-root system user for least-privilege execution (Alpine syntax).
RUN addgroup -S app && adduser -S -G app app

COPY --from=build --chown=app:app /workspace/target/DHBW-Memory-Markus-Wenninger.jar app.jar

USER app

EXPOSE 8080

# BusyBox wget (included in Alpine) — no curl needed.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
