# ============================================================
# eligibility — REST service — port 8084
# ============================================================
# Determines whether the claimant satisfies all eligibility
# requirements by combining results from policy-validation
# and identity-verification.
#
# Depends on both policy-validation and identity-verification
# being healthy before starting.
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

EXPOSE 8084

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
