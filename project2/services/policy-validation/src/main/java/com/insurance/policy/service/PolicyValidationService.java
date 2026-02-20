package com.insurance.policy.service;

import com.insurance.policy.model.PolicyValidationRequest;
import com.insurance.policy.model.PolicyValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PolicyValidationService {

    private static final Logger log = LoggerFactory.getLogger(PolicyValidationService.class);

    // POL-[0-9]{6} -> ACTIVE with full coverage; anything else -> EXPIRED
    private static final Pattern ACTIVE_POLICY_PATTERN = Pattern.compile("^POL-\\d{6}$");
    private static final double ACTIVE_COVERAGE_AMOUNT = 50000.0;

    public PolicyValidationResponse validate(PolicyValidationRequest request) {
        log.debug("Validating policy: policyNumber={}, claimType={}",
                request.policyNumber(), request.claimType());

        boolean matchesPattern = ACTIVE_POLICY_PATTERN.matcher(request.policyNumber()).matches();

        if (matchesPattern) {
            log.info("Policy {} validated successfully as ACTIVE", request.policyNumber());
            return new PolicyValidationResponse(
                    true,
                    "ACTIVE",
                    ACTIVE_COVERAGE_AMOUNT,
                    "Policy is active and eligible for claims. Coverage amount: $" + ACTIVE_COVERAGE_AMOUNT
            );
        }

        log.info("Policy {} failed validation - status EXPIRED", request.policyNumber());
        return new PolicyValidationResponse(
                false,
                "EXPIRED",
                0.0,
                "Policy number " + request.policyNumber()
                        + " does not match a valid active policy format. "
                        + "Expected format: POL-XXXXXX (6 digits)."
        );
    }
}
