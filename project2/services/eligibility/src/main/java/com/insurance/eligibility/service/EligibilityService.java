package com.insurance.eligibility.service;

import com.insurance.eligibility.model.EligibilityRequest;
import com.insurance.eligibility.model.EligibilityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    // Claims at or below $50k are eligible; above that score drops and needs manual review
    private static final double MAX_ELIGIBLE_AMOUNT = 50000.0;
    private static final double HIGH_ELIGIBILITY_SCORE = 0.95;
    private static final double LOW_ELIGIBILITY_SCORE = 0.3;

    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        log.debug("Checking eligibility for claimId={}, estimatedAmount={}",
                request.claimId(), request.estimatedAmount());

        if (request.estimatedAmount() <= MAX_ELIGIBLE_AMOUNT) {
            log.info("Claim {} is eligible. Amount {} within coverage limit.",
                    request.claimId(), request.estimatedAmount());
            return new EligibilityResponse(
                    true,
                    HIGH_ELIGIBILITY_SCORE,
                    "Claim amount is within the maximum coverage limit of $"
                            + MAX_ELIGIBLE_AMOUNT + ". Claim is eligible for processing.",
                    MAX_ELIGIBLE_AMOUNT
            );
        }

        log.info("Claim {} is NOT eligible. Amount {} exceeds coverage limit {}.",
                request.claimId(), request.estimatedAmount(), MAX_ELIGIBLE_AMOUNT);
        return new EligibilityResponse(
                false,
                LOW_ELIGIBILITY_SCORE,
                "Claim amount of $" + request.estimatedAmount()
                        + " exceeds the maximum coverage limit of $" + MAX_ELIGIBLE_AMOUNT
                        + ". Manual review required.",
                MAX_ELIGIBLE_AMOUNT
        );
    }
}
