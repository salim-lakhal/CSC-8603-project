# ============================================================
# claim-tracking — GraphQL service — port 8090
# ============================================================
# Provides a GraphQL API that lets clients query the current
# status and full history of any claim in the system.
# Aggregates state from all upstream services and exposes
# it through a flexible, field-selectable GraphQL schema.
#
# GraphQL endpoint:  http://localhost:8090/graphql
# GraphiQL IDE:      http://localhost:8090/graphiql  (dev profile)
# Actuator health:   http://localhost:8090/actuator/health
#
# Depends on claim-submission being healthy before starting.
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

EXPOSE 8090

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
