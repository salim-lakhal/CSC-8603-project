package com.insurance.fraud.server;

import com.insurance.fraud.service.FraudDetectionServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcServer {

    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    private static final int PORT = 9090;
    private static final long SHUTDOWN_GRACE_PERIOD_SECONDS = 30L;

    private Server server;

    public void start() throws IOException {
        server = ServerBuilder
                .forPort(PORT)
                .addService(new FraudDetectionServiceImpl())
                .build()
                .start();

        log.info("gRPC server is listening on port {}", PORT);
        log.info("Available service: FraudDetectionService");
        log.info("  - AssessFraudRisk  (unary RPC)");
        log.info("  - StreamRiskUpdates (server-streaming RPC)");

        // Graceful shutdown on SIGTERM / SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM shutdown detected — initiating graceful gRPC server shutdown ...");
            try {
                GrpcServer.this.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Shutdown hook interrupted while waiting for server to stop");
            }
            log.info("gRPC server has stopped.");
        }, "grpc-shutdown-hook"));
    }

    public void shutdown() throws InterruptedException {
        if (server == null) {
            return;
        }

        log.info("Shutting down gRPC server (grace period: {}s) ...",
                SHUTDOWN_GRACE_PERIOD_SECONDS);

        server.shutdown();

        boolean terminated = server.awaitTermination(
                SHUTDOWN_GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);

        if (!terminated) {
            log.warn("Grace period elapsed — forcing immediate shutdown.");
            server.shutdownNow();
            server.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
