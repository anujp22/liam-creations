# ── build ────────────────────────────────────────────────────────────────────
# Pinned to the same Java version as <java.version> in pom.xml.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the POM first so dependency resolution is cached independently of source
# changes — edits to src/ then rebuild without re-downloading the world.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests run in CI against the full matrix; skip here so image builds stay fast
# and don't need a database. CI is the gate, not the image build.
RUN mvn -B clean package -DskipTests

# ── run ──────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# curl is needed by the healthcheck below; wget would also do, but curl gives
# clearer failure output in `docker inspect`.
RUN apk add --no-cache curl

# Run as a non-root user. Uploaded files land in /app/uploads, which must be
# writable by this user — in production that path should be an S3 bucket
# instead (see A2), since container-local storage is wiped on every deploy.
RUN addgroup -S catalog && adduser -S catalog -G catalog
WORKDIR /app
COPY --from=build /build/target/catalog-backend-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R catalog:catalog /app
USER catalog

EXPOSE 8080

# Matches management.endpoints.web.exposure.include=health in application.properties.
# start-period gives Flyway time to run migrations on a cold database before the
# first probe counts against us.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
