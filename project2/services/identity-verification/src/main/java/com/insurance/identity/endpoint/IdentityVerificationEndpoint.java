package com.insurance.identity.endpoint;

import com.insurance.identity.model.VerifyIdentityRequest;
import com.insurance.identity.model.VerifyIdentityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.security.SecureRandom;
import java.util.regex.Pattern;

@Endpoint
public class IdentityVerificationEndpoint {

    private static final Logger log = LoggerFactory.getLogger(IdentityVerificationEndpoint.class);

    private static final String NAMESPACE_URI = "http://insurance.com/identity";

    // Expected format: POL-XXXXXX (exactly 6 digits)
    private static final Pattern POLICY_PATTERN = Pattern.compile("^POL-[0-9]{6}$");

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "verifyIdentityRequest")
    @ResponsePayload
    public VerifyIdentityResponse verifyIdentity(@RequestPayload VerifyIdentityRequest request) {

        log.info("Received VerifyIdentity request: {}", request);

        String policyNumber = request.getPolicyNumber();

        if (policyNumber == null || policyNumber.isBlank()) {
            log.warn("Verification FAILED — policy number is null or blank.");
            return new VerifyIdentityResponse("FAILED", "", "Invalid policy number");
        }

        // Format POL-[0-9]{6} is auto-approved
        if (POLICY_PATTERN.matcher(policyNumber).matches()) {
            String verificationCode = generateVerificationCode();
            log.info("Verification VERIFIED — policyNumber='{}', code='{}'",
                    policyNumber, verificationCode);
            return new VerifyIdentityResponse(
                    "VERIFIED",
                    verificationCode,
                    "Identity successfully verified."
            );
        }

        // Valid format but doesn't match the expected pattern — needs manual review
        log.warn("Verification PENDING — policyNumber='{}' did not match expected pattern.",
                policyNumber);
        return new VerifyIdentityResponse("PENDING", "", "Manual review required");
    }

    private String generateVerificationCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return "VC-" + String.format("%06d", code);
    }
}
