package com.insurance.policy.model;

import jakarta.validation.constraints.NotBlank;

public record PolicyValidationRequest(
        @NotBlank(message = "Policy number is required")
        String policyNumber,

        @NotBlank(message = "Claimant name is required")
        String claimantName,

        @NotBlank(message = "Claim type is required")
        String claimType
) {}
