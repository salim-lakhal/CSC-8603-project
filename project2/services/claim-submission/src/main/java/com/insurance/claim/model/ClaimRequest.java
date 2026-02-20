package com.insurance.claim.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(

        @NotBlank(message = "Policy number must not be blank")
        String policyNumber,

        @NotBlank(message = "Claimant name must not be blank")
        String claimantName,

        @NotNull(message = "Incident date is required")
        LocalDate incidentDate,

        @NotBlank(message = "Description must not be blank")
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Estimated amount is required")
        @Positive(message = "Estimated amount must be a positive value")
        BigDecimal estimatedAmount,

        @NotNull(message = "Claim type is required")
        ClaimType claimType

) {}
