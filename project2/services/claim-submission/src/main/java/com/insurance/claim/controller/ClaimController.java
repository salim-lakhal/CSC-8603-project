package com.insurance.claim.controller;

import com.insurance.claim.exception.ClaimNotFoundException;
import com.insurance.claim.model.ClaimRequest;
import com.insurance.claim.model.ClaimResponse;
import com.insurance.claim.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> submitClaim(@Valid @RequestBody ClaimRequest request) {
        ClaimResponse response = claimService.submitClaim(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.claimId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable String id) {
        ClaimResponse response = claimService.getClaim(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        List<ClaimResponse> claims = claimService.getAllClaims();
        return ResponseEntity.ok(claims);
    }
}
