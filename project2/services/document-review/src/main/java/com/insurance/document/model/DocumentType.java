package com.insurance.document.model;

// POLICE_REPORT and MEDICAL_RECORD are auto-validated on submission; others need manual review
public enum DocumentType {
    POLICE_REPORT,
    MEDICAL_RECORD,
    REPAIR_ESTIMATE,
    IDENTITY_PROOF,
    INSURANCE_CARD,
    PHOTO_EVIDENCE
}
