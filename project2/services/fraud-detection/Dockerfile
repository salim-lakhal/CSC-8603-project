# ============================================================
# fraud-detection — gRPC service — port 9090
# ============================================================
# Runs ML-backed fraud scoring on claim data, exposed over
# gRPC (HTTP/2 + Protocol Buffers) on port 9090.
#
# Because standard Docker health checks cannot natively probe
# a gRPC server with a plain curl, this image also starts a
# lightweight HTTP health sidecar on port 9091.
#
# The Spring Boot application should be configured with:
#   grpc.server.port=9090
#   management.server.port=9091        <- actuator on separate port
#   management.endpoints.web.exposure.include=health
#
# Health check in docker-compose probes:
#   http://localhost:9091/health
#
# Build the JAR first:
#   mvn clean package -DskipTests
# ============================================================

FROM openjdk:21-jre-slim

# curl is required for the HTTP health check on port 9091.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/*.jar app.jar

# 9090 — gRPC service port
# 9091 — HTTP management/health port
EXPOSE 9090 9091

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
