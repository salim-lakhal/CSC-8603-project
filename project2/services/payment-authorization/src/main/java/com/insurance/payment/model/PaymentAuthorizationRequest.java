package com.insurance.payment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PaymentAuthorizationRequest(
        @NotBlank(message = "Claim ID is required")
        String claimId,

        @PositiveOrZero(message = "Total payment must be zero or positive")
        double totalPayment,

        @NotBlank(message = "Policy number is required")
        String policyNumber,

        @NotBlank(message = "Bank account is required")
        String bankAccount
) {}
