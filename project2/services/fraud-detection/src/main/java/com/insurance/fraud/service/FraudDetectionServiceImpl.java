package com.insurance.fraud.service;

import com.insurance.fraud.proto.FraudAssessmentRequest;
import com.insurance.fraud.proto.FraudAssessmentResponse;
import com.insurance.fraud.proto.FraudDetectionServiceGrpc;
import com.insurance.fraud.proto.RiskLevel;
import com.insurance.fraud.proto.RiskUpdate;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC service implementation for fraud detection.
 *
 * AssessFraudRisk  — unary RPC, evaluates a single claim
 * StreamRiskUpdates — server-streaming RPC, sends 3 progressive analysis stages
 *
 * Risk scoring rules (AssessFraudRisk):
 *   amount > 100 000  -> CRITICAL (0.95), investigation required
 *   amount > 50 000   -> HIGH (0.80), investigation required
 *   previousClaims > 3 -> MEDIUM (0.60)
 *   amount > 10 000   -> MEDIUM (0.40)
 *   default           -> LOW (0.10)
 */
public class FraudDetectionServiceImpl
        extends FraudDetectionServiceGrpc.FraudDetectionServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionServiceImpl.class);

    @Override
    public void assessFraudRisk(
            FraudAssessmentRequest request,
            StreamObserver<FraudAssessmentResponse> responseObserver) {

        String claimId = request.getClaimId();
        double estimatedAmount = request.getEstimatedAmount();
        int previousClaimsCount = request.getPreviousClaimsCount();

        log.info("[AssessFraudRisk] claimId={} policyNumber={} amount={} claimType={} "
                        + "incidentDate={} previousClaims={}",
                claimId,
                request.getPolicyNumber(),
                estimatedAmount,
                request.getClaimType(),
                request.getIncidentDate(),
                previousClaimsCount);

        try {
            FraudAssessmentResponse response = buildAssessment(
                    claimId, estimatedAmount, previousClaimsCount);

            log.info("[AssessFraudRisk] claimId={} -> riskLevel={} score={} investigation={}",
                    claimId,
                    response.getRiskLevel(),
                    response.getRiskScore(),
                    response.getRequiresInvestigation());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[AssessFraudRisk] Unexpected error for claimId={}", claimId, e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal error during fraud assessment: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException());
        }
    }

    @Override
    public void streamRiskUpdates(
            FraudAssessmentRequest request,
            StreamObserver<RiskUpdate> responseObserver) {

        String claimId = request.getClaimId();
        double estimatedAmount = request.getEstimatedAmount();

        log.info("[StreamRiskUpdates] Starting streaming analysis for claimId={}", claimId);

        try {
            // Stage 1: data validation
            RiskUpdate stage1 = RiskUpdate.newBuilder()
                    .setMessage(String.format(
                            "[Stage 1/3] Claim %s received. Validating data integrity and "
                                    + "cross-referencing policy records ...",
                            claimId))
                    .setCurrentScore(0.10)
                    .build();

            log.debug("[StreamRiskUpdates] Sending stage 1 update for claimId={}", claimId);
            responseObserver.onNext(stage1);
            Thread.sleep(500);

            // Stage 2: behavioural pattern analysis
            double intermediateScore = computeIntermediateScore(
                    estimatedAmount, request.getPreviousClaimsCount());

            RiskUpdate stage2 = RiskUpdate.newBuilder()
                    .setMessage(String.format(
                            "[Stage 2/3] Claim %s — analysing historical patterns. "
                                    + "Amount: %.2f | Previous claims: %d | Intermediate score: %.2f",
                            claimId,
                            estimatedAmount,
                            request.getPreviousClaimsCount(),
                            intermediateScore))
                    .setCurrentScore(intermediateScore)
                    .build();

            log.debug("[StreamRiskUpdates] Sending stage 2 update for claimId={}, score={}",
                    claimId, intermediateScore);
            responseObserver.onNext(stage2);
            Thread.sleep(500);

            // Stage 3: final score
            FraudAssessmentResponse finalAssessment = buildAssessment(
                    claimId, estimatedAmount, request.getPreviousClaimsCount());

            RiskUpdate stage3 = RiskUpdate.newBuilder()
                    .setMessage(String.format(
                            "[Stage 3/3] Claim %s — analysis complete. "
                                    + "Final risk level: %s | Score: %.2f | Requires investigation: %b",
                            claimId,
                            finalAssessment.getRiskLevel().name(),
                            finalAssessment.getRiskScore(),
                            finalAssessment.getRequiresInvestigation()))
                    .setCurrentScore(finalAssessment.getRiskScore())
                    .build();

            log.debug("[StreamRiskUpdates] Sending stage 3 (final) update for claimId={}", claimId);
            responseObserver.onNext(stage3);
            Thread.sleep(500);

            log.info("[StreamRiskUpdates] Stream completed for claimId={}", claimId);
            responseObserver.onCompleted();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[StreamRiskUpdates] Stream interrupted for claimId={}", claimId);
            responseObserver.onError(
                    Status.CANCELLED
                            .withDescription("Risk update stream was interrupted for claim: " + claimId)
                            .withCause(e)
                            .asRuntimeException());
        } catch (Exception e) {
            log.error("[StreamRiskUpdates] Unexpected error for claimId={}", claimId, e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal error during risk streaming: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException());
        }
    }

    private FraudAssessmentResponse buildAssessment(
            String claimId,
            double estimatedAmount,
            int previousClaimsCount) {

        RiskLevel riskLevel;
        double riskScore;
        String assessmentReason;
        boolean requiresInvestigation;

        if (estimatedAmount > 100_000) {
            riskLevel = RiskLevel.CRITICAL;
            riskScore = 0.95;
            assessmentReason = "Extremely high claim amount";
            requiresInvestigation = true;

        } else if (estimatedAmount > 50_000) {
            riskLevel = RiskLevel.HIGH;
            riskScore = 0.80;
            assessmentReason = "High claim amount";
            requiresInvestigation = true;

        } else if (previousClaimsCount > 3) {
            riskLevel = RiskLevel.MEDIUM;
            riskScore = 0.60;
            assessmentReason = "Multiple previous claims";
            requiresInvestigation = false;

        } else if (estimatedAmount > 10_000) {
            riskLevel = RiskLevel.MEDIUM;
            riskScore = 0.40;
            assessmentReason = "Moderate claim amount";
            requiresInvestigation = false;

        } else {
            riskLevel = RiskLevel.LOW;
            riskScore = 0.10;
            assessmentReason = "Standard claim";
            requiresInvestigation = false;
        }

        return FraudAssessmentResponse.newBuilder()
                .setClaimId(claimId)
                .setRiskLevel(riskLevel)
                .setRiskScore(riskScore)
                .setAssessmentReason(assessmentReason)
                .setRequiresInvestigation(requiresInvestigation)
                .build();
    }

    private double computeIntermediateScore(double estimatedAmount, int previousClaimsCount) {
        // Amount signal: map to [0, 0.7] capped at 100 000
        double amountSignal = Math.min(estimatedAmount / 100_000.0, 1.0) * 0.70;

        // Frequency signal: each claim above baseline adds 0.06, capped at 0.30
        double frequencySignal = Math.min(Math.max(previousClaimsCount - 1, 0) * 0.06, 0.30);

        return Math.round((amountSignal + frequencySignal) * 100.0) / 100.0;
    }
}
