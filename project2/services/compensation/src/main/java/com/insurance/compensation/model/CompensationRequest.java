package com.insurance.compensation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CompensationRequest(
        @NotBlank(message = "Claim ID is required")
        String claimId,

        @PositiveOrZero(message = "Approved amount must be zero or positive")
        double approvedAmount,

        @NotBlank(message = "Claim type is required")
        String claimType,

        @PositiveOrZero(message = "Deductible must be zero or positive")
        double deductible
) {}
