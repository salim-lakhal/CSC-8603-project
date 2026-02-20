package com.insurance.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// SOAP/WSDL service — WSDL available at http://localhost:8082/ws/identity.wsdl
@SpringBootApplication
public class IdentityVerificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityVerificationApplication.class, args);
    }
}
