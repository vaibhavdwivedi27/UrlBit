# ==============================================================================
# Urlbit Backend Multi-Stage Production Dockerfile
#
# Stage 1: Build the Spring Boot application using Maven
# Stage 2: Extract jar layers with Spring Boot layertools for optimal Docker caching
# Stage 3: Minimal, secure JRE runtime running as non-root user
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Builder
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Install dos2unix to ensure Unix line endings for mvnw inside Alpine
RUN apk add --no-cache bash dos2unix

# Copy Maven wrapper and POM first for layer-cached dependency downloads
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN dos2unix mvnw && chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build production jar (unit/integration tests ran in CI)
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Layer Extractor
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /extracted

COPY --from=builder /workspace/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ------------------------------------------------------------------------------
# Stage 3: Final Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Install curl/wget for container health checks
RUN apk add --no-cache curl

# Create unprivileged user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy layers in order of modification frequency (least to most frequent)
COPY --from=extractor /extracted/dependencies/ ./
COPY --from=extractor /extracted/spring-boot-loader/ ./
COPY --from=extractor /extracted/snapshot-dependencies/ ./
COPY --from=extractor /extracted/application/ ./

# Switch to unprivileged user
USER appuser

EXPOSE 8080

# Container liveness check via Actuator liveness probe
HEALTHCHECK --interval=10s --timeout=3s --retries=3 --start-period=20s \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Container-aware JVM options: limit heap to 75% container memory, fast exit on OOM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
