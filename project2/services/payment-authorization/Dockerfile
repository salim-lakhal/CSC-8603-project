# ============================================================
# payment-authorization — REST service — port 8088
# ============================================================
# Authorizes the financial transfer of the approved
# compensation amount and initiates the payment instruction
# toward the banking/payment gateway.
#
# Depends on compensation being healthy before starting.
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

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
