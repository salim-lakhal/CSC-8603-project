package com.insurance.tracking.model;

// Represents every stage a claim can pass through in the processing pipeline
public enum Status {
    SUBMITTED,
    IDENTITY_VERIFIED,
    POLICY_VALIDATED,
    FRAUD_CHECKED,
    ELIGIBILITY_CONFIRMED,
    DOCUMENTS_REVIEWED,
    EXPERT_ASSESSED,
    COMPENSATION_CALCULATED,
    PAYMENT_AUTHORIZED,
    NOTIFIED,
    COMPLETED,
    REJECTED
}
