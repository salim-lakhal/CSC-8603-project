package com.insurance.eligibility.controller;

import com.insurance.eligibility.model.EligibilityRequest;
import com.insurance.eligibility.model.EligibilityResponse;
import com.insurance.eligibility.service.EligibilityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eligibility")
public class EligibilityController {

    private static final Logger log = LoggerFactory.getLogger(EligibilityController.class);

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @PostMapping("/check")
    public ResponseEntity<EligibilityResponse> checkEligibility(
            @Valid @RequestBody EligibilityRequest request) {

        log.info("Received eligibility check for claimId={}, policyNumber={}",
                request.claimId(), request.policyNumber());

        EligibilityResponse response = eligibilityService.checkEligibility(request);
        return ResponseEntity.ok(response);
    }
}
