# ============================================================
# expert-assessment — REST service — port 8086
# ============================================================
# Assigns claims to domain experts (medical, legal, technical)
# and aggregates their assessments into a structured verdict.
# Only triggered after fraud-detection and eligibility checks
# have both passed successfully.
#
# Depends on fraud-detection and eligibility being healthy.
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

EXPOSE 8086

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
