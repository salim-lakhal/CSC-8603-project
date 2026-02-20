package com.insurance.compensation.model;

public record CompensationResponse(
        String claimId,
        double grossAmount,
        double deductible,
        double netAmount,
        double taxAmount,
        double totalPayment
) {}
