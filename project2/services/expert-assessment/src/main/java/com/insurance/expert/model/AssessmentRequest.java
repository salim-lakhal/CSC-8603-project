package com.insurance.expert.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AssessmentRequest(
        @NotBlank(message = "Claim ID is required")
        String claimId,

        @NotBlank(message = "Claim type is required")
        String claimType,

        @Positive(message = "Estimated amount must be positive")
        double estimatedAmount,

        @NotBlank(message = "Description is required")
        String description
) {}
