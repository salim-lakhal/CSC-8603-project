package com.insurance.payment.model;

public record PaymentAuthorizationResponse(
        String authorizationId,
        String claimId,
        String status,
        String authorizationCode,
        String message
) {}
