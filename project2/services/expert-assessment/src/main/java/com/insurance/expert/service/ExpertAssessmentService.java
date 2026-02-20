package com.insurance.expert.service;

import com.insurance.expert.model.AssessmentRequest;
import com.insurance.expert.model.AssessmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExpertAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(ExpertAssessmentService.class);

    private static final double HIGH_VALUE_THRESHOLD = 100_000.0;
    private static final double MEDIUM_VALUE_THRESHOLD = 50_000.0;
    private static final double HIGH_VALUE_APPROVAL_RATE = 0.80;
    private static final double STANDARD_APPROVAL_RATE = 0.90;

    // Simulated assessor pool — in production this would come from a database or assignment service
    private static final String[] ASSESSORS = {
            "Dr. Marie Dupont", "Jean-Pierre Martin", "Sophie Bernard", "Luc Moreau"
    };

    /**
     * Assessment rules:
     *   amount > 100 000  -> REVIEW (committee approval required)
     *   amount > 50 000   -> APPROVE at 80%
     *   amount <= 50 000  -> APPROVE at 90%
     */
    public AssessmentResponse assess(AssessmentRequest request) {
        log.debug("Processing assessment for claimId={}, estimatedAmount={}",
                request.claimId(), request.estimatedAmount());

        String assessmentId = UUID.randomUUID().toString();
        String assessorName = selectAssessor(request.claimId());

        if (request.estimatedAmount() > HIGH_VALUE_THRESHOLD) {
            log.info("Claim {} flagged for REVIEW - amount {} exceeds high-value threshold {}",
                    request.claimId(), request.estimatedAmount(), HIGH_VALUE_THRESHOLD);
            return new AssessmentResponse(
                    assessmentId,
                    request.claimId(),
                    0.0,
                    assessorName,
                    "REVIEW",
                    "Claim amount of $" + request.estimatedAmount()
                            + " exceeds the high-value threshold of $" + HIGH_VALUE_THRESHOLD
                            + ". Requires committee review before approval."
            );
        }

        if (request.estimatedAmount() > MEDIUM_VALUE_THRESHOLD) {
            double approvedAmount = request.estimatedAmount() * HIGH_VALUE_APPROVAL_RATE;
            log.info("Claim {} APPROVED at 80% rate. Approved amount: {}",
                    request.claimId(), approvedAmount);
            return new AssessmentResponse(
                    assessmentId,
                    request.claimId(),
                    approvedAmount,
                    assessorName,
                    "APPROVE",
                    "Claim amount of $" + request.estimatedAmount()
                            + " is above $" + MEDIUM_VALUE_THRESHOLD
                            + ". Approved at 80% rate: $" + String.format("%.2f", approvedAmount) + "."
            );
        }

        double approvedAmount = request.estimatedAmount() * STANDARD_APPROVAL_RATE;
        log.info("Claim {} APPROVED at standard 90% rate. Approved amount: {}",
                request.claimId(), approvedAmount);
        return new AssessmentResponse(
                assessmentId,
                request.claimId(),
                approvedAmount,
                assessorName,
                "APPROVE",
                "Standard claim approved at 90% rate: $" + String.format("%.2f", approvedAmount) + "."
        );
    }

    // Deterministic assessor assignment based on claim ID hash
    private String selectAssessor(String claimId) {
        int index = Math.abs(claimId.hashCode()) % ASSESSORS.length;
        return ASSESSORS[index];
    }
}
