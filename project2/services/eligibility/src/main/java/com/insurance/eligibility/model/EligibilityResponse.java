package com.insurance.eligibility.model;

public record EligibilityResponse(
        boolean eligible,
        double eligibilityScore,
        String reason,
        double maxCoverage
) {}
