package com.insurance.policy.controller;

import com.insurance.policy.model.PolicyValidationRequest;
import com.insurance.policy.model.PolicyValidationResponse;
import com.insurance.policy.service.PolicyValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/policies")
public class PolicyValidationController {

    private static final Logger log = LoggerFactory.getLogger(PolicyValidationController.class);

    private final PolicyValidationService policyValidationService;

    public PolicyValidationController(PolicyValidationService policyValidationService) {
        this.policyValidationService = policyValidationService;
    }

    @PostMapping("/validate")
    public ResponseEntity<PolicyValidationResponse> validatePolicy(
            @Valid @RequestBody PolicyValidationRequest request) {

        log.info("Received policy validation request for policyNumber={}",
                request.policyNumber());

        PolicyValidationResponse response = policyValidationService.validate(request);
        return ResponseEntity.ok(response);
    }
}
