package com.insurance.expert.model;

public record AssessmentResponse(
        String assessmentId,
        String claimId,
        double approvedAmount,
        String assessorName,
        String recommendation,
        String notes
) {}
