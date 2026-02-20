package com.insurance.fraud;

import com.insurance.fraud.server.GrpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FraudDetectionApplication {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionApplication.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== Insurance Fraud Detection Service ===");
        log.info("Starting gRPC server on port 9090 ...");

        GrpcServer server = new GrpcServer();

        try {
            server.start();
            log.info("Server started successfully. Awaiting incoming requests.");
            server.blockUntilShutdown();
        } catch (Exception e) {
            log.error("Fatal error while running gRPC server", e);
            System.exit(1);
        }
    }
}
