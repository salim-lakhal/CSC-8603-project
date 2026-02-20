package com.insurance.tracking.controller;

import com.insurance.tracking.model.ClaimStatus;
import com.insurance.tracking.model.Status;
import com.insurance.tracking.service.ClaimStatusService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ClaimTrackingController {

    private final ClaimStatusService claimStatusService;

    public ClaimTrackingController(ClaimStatusService claimStatusService) {
        this.claimStatusService = claimStatusService;
    }

    // Queries

    @QueryMapping
    public ClaimStatus trackClaim(@Argument String claimId) {
        return claimStatusService.trackClaim(claimId);
    }

    @QueryMapping
    public List<ClaimStatus> getClaimsByStatus(@Argument Status status) {
        return claimStatusService.getClaimsByStatus(status);
    }

    @QueryMapping
    public List<ClaimStatus> getAllClaims() {
        return claimStatusService.getAllClaims();
    }

    // Mutations

    @MutationMapping
    public ClaimStatus initializeClaim(@Argument String claimId) {
        return claimStatusService.initializeClaim(claimId);
    }

    @MutationMapping
    public ClaimStatus updateClaimStatus(
            @Argument String claimId,
            @Argument Status status,
            @Argument String description,
            @Argument String updatedBy) {
        return claimStatusService.updateClaimStatus(claimId, status, description, updatedBy);
    }
}
