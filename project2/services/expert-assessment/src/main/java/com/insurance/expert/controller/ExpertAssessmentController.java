package com.insurance.expert.controller;

import com.insurance.expert.model.AssessmentRequest;
import com.insurance.expert.model.AssessmentResponse;
import com.insurance.expert.service.ExpertAssessmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assessments")
public class ExpertAssessmentController {

    private static final Logger log = LoggerFactory.getLogger(ExpertAssessmentController.class);

    private final ExpertAssessmentService expertAssessmentService;

    public ExpertAssessmentController(ExpertAssessmentService expertAssessmentService) {
        this.expertAssessmentService = expertAssessmentService;
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> createAssessment(
            @Valid @RequestBody AssessmentRequest request) {

        log.info("Received assessment request for claimId={}, claimType={}",
                request.claimId(), request.claimType());

        AssessmentResponse response = expertAssessmentService.assess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
