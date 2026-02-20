package com.insurance.identity.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "verifyIdentityResponse", namespace = "http://insurance.com/identity")
@XmlAccessorType(XmlAccessType.FIELD)
public class VerifyIdentityResponse {

    // One of: VERIFIED, FAILED, PENDING
    @XmlElement(name = "verificationStatus", namespace = "http://insurance.com/identity", required = true)
    private String verificationStatus;

    @XmlElement(name = "verificationCode", namespace = "http://insurance.com/identity", required = true)
    private String verificationCode;

    @XmlElement(name = "message", namespace = "http://insurance.com/identity", required = true)
    private String message;

    // Required by JAXB
    public VerifyIdentityResponse() {
    }

    public VerifyIdentityResponse(String verificationStatus, String verificationCode, String message) {
        this.verificationStatus = verificationStatus;
        this.verificationCode = verificationCode;
        this.message = message;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "VerifyIdentityResponse{"
                + "verificationStatus='" + verificationStatus + '\''
                + ", verificationCode='" + verificationCode + '\''
                + ", message='" + message + '\''
                + '}';
    }
}
