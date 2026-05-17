# syntax=docker/dockerfile:1
#
# DHBW Memory — multi-stage Docker build.
#
# Stage 1 builds the production jar (Vaadin frontend bundled by Vite +
# Spring Boot fat jar). Stage 2 ships only the JRE + the jar, keeping the
# runtime image small and free of build tooling.

# ── Build stage ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the build descriptors first; this layer is cached between code-only
# changes so dependencies don't have to be re-resolved on every rebuild.
COPY pom.xml ./
COPY package.json ./
COPY vite.config.ts ./

COPY src ./src

# -P production = optimized frontend bundle + dev jar excluded (see pom.xml).
RUN mvn -B -DskipTests package -P production

# ── Runtime stage ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/DHBW_Memory-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
