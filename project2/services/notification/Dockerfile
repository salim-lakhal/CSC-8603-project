# ============================================================
# notification — REST service — port 8089
# ============================================================
# Sends claimant-facing notifications (email, SMS, push) at
# each milestone of the claim lifecycle: submission received,
# under review, approved, payment dispatched, etc.
#
# Depends on payment-authorization being healthy before
# starting, ensuring notifications fire only after the full
# upstream pipeline is confirmed operational.
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

EXPOSE 8089

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
