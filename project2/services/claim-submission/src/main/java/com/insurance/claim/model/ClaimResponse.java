package com.insurance.claim.model;

import java.time.LocalDateTime;

public record ClaimResponse(

        String claimId,
        ClaimStatus status,
        String message,
        LocalDateTime submissionTimestamp,
        String policyNumber

) {}
