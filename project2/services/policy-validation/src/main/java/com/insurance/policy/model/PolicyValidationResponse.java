package com.insurance.policy.model;

public record PolicyValidationResponse(
        boolean valid,
        String policyStatus,
        double coverageAmount,
        String message
) {}
