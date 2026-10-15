# Stage 1: Build JAR File using Eclipse Temurin JDK 21 Alpine
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache Gradle wrapper and dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build executable bootJar
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Minimalist Lightweight JRE 21 Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-privileged system user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+OptimizeStringConcat", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
