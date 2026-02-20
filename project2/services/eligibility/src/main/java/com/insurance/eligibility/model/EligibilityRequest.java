package com.insurance.eligibility.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record EligibilityRequest(
        @NotBlank(message = "Claim ID is required")
        String claimId,

        @NotBlank(message = "Policy number is required")
        String policyNumber,

        @NotBlank(message = "Claim type is required")
        String claimType,

        @Positive(message = "Estimated amount must be positive")
        double estimatedAmount
) {}
