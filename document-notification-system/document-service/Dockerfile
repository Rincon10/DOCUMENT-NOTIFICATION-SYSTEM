# syntax=docker/dockerfile:1

### --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-19 AS builder
WORKDIR /app

COPY . .

# The BuildKit cache mount keeps the local Maven repo between builds, so
# dependencies are only downloaded once; -pl/-am builds just this service's
# runnable module plus the modules it depends on.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package -pl document-service/document-container -am

### --- Stage 2: Runtime ---
FROM eclipse-temurin:19-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder --chown=appuser:appgroup /app/document-service/document-container/target/document-container-*.jar app.jar

EXPOSE 8181

# Container-aware JVM defaults; override JAVA_OPTS per environment.
# Spring profile is NOT hardcoded: set SPRING_PROFILES_ACTIVE (e.g. "docker")
# via the orchestrator, or run with the env-driven base configuration.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO /dev/null "http://localhost:${SERVER_PORT:-8181}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
