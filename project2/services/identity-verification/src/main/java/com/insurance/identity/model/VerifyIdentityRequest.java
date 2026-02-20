package com.insurance.identity.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "verifyIdentityRequest", namespace = "http://insurance.com/identity")
@XmlAccessorType(XmlAccessType.FIELD)
public class VerifyIdentityRequest {

    @XmlElement(name = "policyNumber", namespace = "http://insurance.com/identity", required = true)
    private String policyNumber;

    @XmlElement(name = "claimantName", namespace = "http://insurance.com/identity", required = true)
    private String claimantName;

    @XmlElement(name = "dateOfBirth", namespace = "http://insurance.com/identity", required = true)
    private String dateOfBirth;

    // Required by JAXB
    public VerifyIdentityRequest() {
    }

    public VerifyIdentityRequest(String policyNumber, String claimantName, String dateOfBirth) {
        this.policyNumber = policyNumber;
        this.claimantName = claimantName;
        this.dateOfBirth = dateOfBirth;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    public String toString() {
        return "VerifyIdentityRequest{"
                + "policyNumber='" + policyNumber + '\''
                + ", claimantName='" + claimantName + '\''
                + ", dateOfBirth='" + dateOfBirth + '\''
                + '}';
    }
}
