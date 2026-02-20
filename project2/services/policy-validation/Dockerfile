# ============================================================
# policy-validation — REST service — port 8083
# ============================================================
# Validates that the submitted claim falls within the scope
# of the claimant's active insurance policy (coverage type,
# validity period, exclusions).
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

EXPOSE 8083

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
