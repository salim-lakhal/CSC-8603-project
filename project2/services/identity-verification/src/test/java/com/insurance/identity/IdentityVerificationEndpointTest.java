package com.insurance.identity;

import com.insurance.identity.endpoint.IdentityVerificationEndpoint;
import com.insurance.identity.model.VerifyIdentityRequest;
import com.insurance.identity.model.VerifyIdentityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdentityVerificationEndpoint unit tests")
class IdentityVerificationEndpointTest {

    private IdentityVerificationEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new IdentityVerificationEndpoint();
    }

    // VERIFIED path

    @Test
    @DisplayName("Valid POL-XXXXXX policy number returns VERIFIED status")
    void validPolicyNumber_returnsVerified() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "POL-123456", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("VERIFIED");
        assertThat(response.getVerificationCode()).matches("VC-\\d{6}");
        assertThat(response.getMessage()).isEqualTo("Identity successfully verified.");
    }

    @Test
    @DisplayName("Each valid request produces a different verification code (probabilistic)")
    void verifiedResponses_produceDifferentCodes() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "POL-000001", "John Smith", "1990-01-01");

        String code1 = endpoint.verifyIdentity(request).getVerificationCode();
        String code2 = endpoint.verifyIdentity(request).getVerificationCode();
        String code3 = endpoint.verifyIdentity(request).getVerificationCode();

        // With 1,000,000 possible codes, three identical values would be astronomically unlikely
        assertThat(java.util.Set.of(code1, code2, code3)).hasSizeGreaterThan(1);
    }

    // FAILED path

    @Test
    @DisplayName("Null policy number returns FAILED status")
    void nullPolicyNumber_returnsFailed() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                null, "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("FAILED");
        assertThat(response.getVerificationCode()).isEmpty();
        assertThat(response.getMessage()).isEqualTo("Invalid policy number");
    }

    @Test
    @DisplayName("Blank policy number returns FAILED status")
    void blankPolicyNumber_returnsFailed() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "   ", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("FAILED");
        assertThat(response.getVerificationCode()).isEmpty();
        assertThat(response.getMessage()).isEqualTo("Invalid policy number");
    }

    @Test
    @DisplayName("Empty string policy number returns FAILED status")
    void emptyPolicyNumber_returnsFailed() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("FAILED");
        assertThat(response.getVerificationCode()).isEmpty();
    }

    // PENDING path

    @Test
    @DisplayName("Policy number with wrong format returns PENDING status")
    void wrongFormatPolicyNumber_returnsPending() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "INV-ABCDEF", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(response.getVerificationCode()).isEmpty();
        assertThat(response.getMessage()).isEqualTo("Manual review required");
    }

    @Test
    @DisplayName("Policy number with too few digits returns PENDING status")
    void tooFewDigits_returnsPending() {
        // POL-12345 has only 5 digits — must not match
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "POL-12345", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Policy number with too many digits returns PENDING status")
    void tooManyDigits_returnsPending() {
        // POL-1234567 has 7 digits — must not match
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "POL-1234567", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Policy number with letters in digit section returns PENDING status")
    void lettersInDigitSection_returnsPending() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "POL-12345A", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Lowercase pol- prefix returns PENDING (case-sensitive match)")
    void lowercasePrefix_returnsPending() {
        VerifyIdentityRequest request = new VerifyIdentityRequest(
                "pol-123456", "Jane Doe", "1985-07-22");

        VerifyIdentityResponse response = endpoint.verifyIdentity(request);

        assertThat(response.getVerificationStatus()).isEqualTo("PENDING");
    }
}
