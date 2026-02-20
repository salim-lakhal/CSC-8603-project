package com.insurance.claim.service;

import com.insurance.claim.exception.ClaimNotFoundException;
import com.insurance.claim.model.ClaimRequest;
import com.insurance.claim.model.ClaimResponse;
import com.insurance.claim.model.ClaimStatus;
import com.insurance.claim.repository.ClaimRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;

    public ClaimService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public ClaimResponse submitClaim(ClaimRequest request) {
        String claimId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        claimRepository.save(claimId, request);

        return new ClaimResponse(
                claimId,
                ClaimStatus.SUBMITTED,
                "Claim submitted successfully. Reference ID: " + claimId,
                now,
                request.policyNumber()
        );
    }

    public Optional<ClaimResponse> getClaim(String id) {
        return claimRepository.findById(id)
                .map(request -> new ClaimResponse(
                        id,
                        ClaimStatus.SUBMITTED,
                        "Claim retrieved successfully",
                        null,
                        request.policyNumber()
                ));
    }

    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(entry -> new ClaimResponse(
                        entry.getKey(),
                        ClaimStatus.SUBMITTED,
                        "Claim retrieved successfully",
                        null,
                        entry.getValue().policyNumber()
                ))
                .toList();
    }
}
