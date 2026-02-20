# ============================================================
# compensation — REST service — port 8087
# ============================================================
# Calculates the final compensation amount owed to the
# claimant based on the expert assessment verdict and the
# coverage limits defined in the policy.
#
# Depends on expert-assessment being healthy before starting.
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

EXPOSE 8087

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
