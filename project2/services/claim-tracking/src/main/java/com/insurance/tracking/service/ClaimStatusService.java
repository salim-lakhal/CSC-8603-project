package com.insurance.tracking.service;

import com.insurance.tracking.model.ClaimStatus;
import com.insurance.tracking.model.Status;
import com.insurance.tracking.model.StatusEntry;
import com.insurance.tracking.repository.ClaimStatusRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClaimStatusService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    private final ClaimStatusRepository claimStatusRepository;

    public ClaimStatusService(ClaimStatusRepository claimStatusRepository) {
        this.claimStatusRepository = claimStatusRepository;
    }

    public ClaimStatus trackClaim(String claimId) {
        return claimStatusRepository.findByClaimId(claimId).orElse(null);
    }

    public List<ClaimStatus> getClaimsByStatus(Status status) {
        return claimStatusRepository.findByCurrentStatus(status);
    }

    public List<ClaimStatus> getAllClaims() {
        return claimStatusRepository.findAll();
    }

    public ClaimStatus initializeClaim(String claimId) {
        if (claimStatusRepository.exists(claimId)) {
            throw new IllegalArgumentException(
                "Claim already exists with id: " + claimId +
                ". Use updateClaimStatus to advance its state.");
        }

        String now = Instant.now().toString();

        StatusEntry submittedEntry = new StatusEntry(
            Status.SUBMITTED,
            now,
            "Claim initialized and registered in the tracking system.",
            "SYSTEM"
        );

        List<StatusEntry> history = new ArrayList<>();
        history.add(submittedEntry);

        ClaimStatus claimStatus = new ClaimStatus(
            claimId,
            Status.SUBMITTED,
            history,
            now,
            calculateEstimatedCompletion(Status.SUBMITTED)
        );

        return claimStatusRepository.save(claimStatus);
    }

    public ClaimStatus updateClaimStatus(String claimId,
                                         Status status,
                                         String description,
                                         String updatedBy) {
        ClaimStatus claimStatus = claimStatusRepository.findByClaimId(claimId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Claim not found with id: " + claimId +
                ". Use initializeClaim to create a new claim first."));

        String now = Instant.now().toString();

        StatusEntry newEntry = new StatusEntry(status, now, description, updatedBy);
        claimStatus.addStatusEntry(newEntry);
        claimStatus.setCurrentStatus(status);
        claimStatus.setLastUpdated(now);
        claimStatus.setEstimatedCompletionDate(calculateEstimatedCompletion(status));

        return claimStatusRepository.save(claimStatus);
    }

    // Estimate completion date based on pipeline position
    private String calculateEstimatedCompletion(Status status) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return switch (status) {
            case SUBMITTED,
                 IDENTITY_VERIFIED,
                 POLICY_VALIDATED,
                 FRAUD_CHECKED,
                 ELIGIBILITY_CONFIRMED -> today.plusDays(15).format(ISO_DATE);

            case DOCUMENTS_REVIEWED,
                 EXPERT_ASSESSED,
                 COMPENSATION_CALCULATED -> today.plusDays(7).format(ISO_DATE);

            case PAYMENT_AUTHORIZED,
                 NOTIFIED -> today.plusDays(2).format(ISO_DATE);

            case COMPLETED,
                 REJECTED -> null;
        };
    }
}
