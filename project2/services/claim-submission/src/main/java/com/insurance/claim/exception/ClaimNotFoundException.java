package com.insurance.claim.exception;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(String claimId) {
        super("Claim not found with ID: " + claimId);
    }
}
