# ============================================================
# document-review — GraphQL service — port 8085
# ============================================================
# Exposes a GraphQL API (via Spring for GraphQL) for querying
# and mutating claim-related documents such as photos,
# expert reports, and invoices.
#
# GraphQL endpoint:  http://localhost:8085/graphql
# GraphiQL IDE:      http://localhost:8085/graphiql  (dev profile)
# Actuator health:   http://localhost:8085/actuator/health
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

EXPOSE 8085

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
