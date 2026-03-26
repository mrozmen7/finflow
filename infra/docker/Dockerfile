# ============================================================
# Stage 1 — Build
# Uses full JDK to compile and package the application.
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copy Maven wrapper and POM first so the dependency layer is
# cached independently of source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and produce the fat JAR.
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# ============================================================
# Stage 2 — Run
# Uses JRE-only image to minimise attack surface and image size.
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS run

WORKDIR /app

# curl is not included in alpine by default; required for HEALTHCHECK.
RUN apk add --no-cache curl

# Run as a dedicated non-root user.
RUN addgroup -S finflow && adduser -S finflow -G finflow

# Copy only the fat JAR from the build stage.
COPY --from=build /build/target/finflow-*.jar app.jar
RUN chown finflow:finflow app.jar

USER finflow

EXPOSE 8080

# Wait 60 s for Spring Boot to start before the first probe.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

# -XX:MaxRAMPercentage: cap heap at 75 % of the container memory limit.
# -Djava.security.egd: use non-blocking entropy to prevent startup delay.
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
