package com.insurance.compensation.controller;

import com.insurance.compensation.model.CompensationRequest;
import com.insurance.compensation.model.CompensationResponse;
import com.insurance.compensation.service.CompensationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/compensation")
public class CompensationController {

    private static final Logger log = LoggerFactory.getLogger(CompensationController.class);

    private final CompensationService compensationService;

    public CompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<CompensationResponse> calculateCompensation(
            @Valid @RequestBody CompensationRequest request) {

        log.info("Received compensation calculation request for claimId={}", request.claimId());

        CompensationResponse response = compensationService.calculate(request);
        return ResponseEntity.ok(response);
    }
}
