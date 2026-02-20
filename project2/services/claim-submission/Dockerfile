# ============================================================
# claim-submission — REST service — port 8081
# ============================================================
# Accepts incoming insurance claim payloads and acts as the
# system entry point for the claim processing pipeline.
#
# Build the JAR first:
#   mvn clean package -DskipTests
# Then let docker-compose build this image:
#   docker-compose up --build
# ============================================================

FROM openjdk:21-jre-slim

# Install curl so the Docker health check can probe
# the Spring Boot Actuator /actuator/health endpoint.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the fat JAR produced by Maven into the image.
COPY target/*.jar app.jar

EXPOSE 8081

# JAVA_OPTS is injected at runtime via the docker-compose
# environment block (default: empty string).
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
