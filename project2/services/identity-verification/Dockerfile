# ============================================================
# identity-verification — SOAP/WSDL service — port 8082
# ============================================================
# Exposes a SOAP endpoint described by a WSDL contract.
# Verifies claimant identity against external identity
# registries using XML-based message exchange.
#
# The Spring Boot Actuator health endpoint is available at:
#   http://localhost:8082/actuator/health
#
# Build the JAR first:
#   mvn clean package -DskipTests
# ============================================================

FROM openjdk:21-jre-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
