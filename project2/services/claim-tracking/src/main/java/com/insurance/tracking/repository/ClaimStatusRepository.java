package com.insurance.tracking.repository;

import com.insurance.tracking.model.ClaimStatus;
import com.insurance.tracking.model.Status;
import com.insurance.tracking.model.StatusEntry;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class ClaimStatusRepository {

    private final Map<String, ClaimStatus> store = new ConcurrentHashMap<>();

    public ClaimStatusRepository() {
        seedSampleData();
    }

    private void seedSampleData() {

        // CLM-2024-001: well-advanced through the pipeline
        List<StatusEntry> history1 = new ArrayList<>(Arrays.asList(
            new StatusEntry(
                Status.SUBMITTED,
                "2024-03-10T08:00:00Z",
                "Claim received via online portal. Reference number assigned.",
                "PORTAL_SYSTEM"
            ),
            new StatusEntry(
                Status.IDENTITY_VERIFIED,
                "2024-03-10T09:15:00Z",
                "Identity confirmed via national ID database cross-reference.",
                "ID_VERIFICATION_SERVICE"
            ),
            new StatusEntry(
                Status.POLICY_VALIDATED,
                "2024-03-10T10:30:00Z",
                "Active policy confirmed. Coverage applies to the reported incident type.",
                "POLICY_SERVICE"
            ),
            new StatusEntry(
                Status.FRAUD_CHECKED,
                "2024-03-11T11:00:00Z",
                "Anti-fraud analysis completed. No anomalies detected.",
                "FRAUD_DETECTION_SERVICE"
            ),
            new StatusEntry(
                Status.ELIGIBILITY_CONFIRMED,
                "2024-03-11T14:00:00Z",
                "Claimant confirmed eligible. Deductible applied: 500 EUR.",
                "ADJUSTER_MARTIN"
            ),
            new StatusEntry(
                Status.DOCUMENTS_REVIEWED,
                "2024-03-12T09:00:00Z",
                "Police report and repair estimate reviewed and accepted.",
                "ADJUSTER_MARTIN"
            )
        ));

        ClaimStatus claim1 = new ClaimStatus(
            "CLM-2024-001",
            Status.DOCUMENTS_REVIEWED,
            history1,
            "2024-03-12T09:00:00Z",
            "2024-03-20T00:00:00Z"
        );

        // CLM-2024-002: recently validated
        List<StatusEntry> history2 = new ArrayList<>(Arrays.asList(
            new StatusEntry(
                Status.SUBMITTED,
                "2024-03-14T13:00:00Z",
                "Claim submitted by claimant via mobile application.",
                "MOBILE_APP"
            ),
            new StatusEntry(
                Status.IDENTITY_VERIFIED,
                "2024-03-14T14:00:00Z",
                "Identity verified successfully.",
                "ID_VERIFICATION_SERVICE"
            ),
            new StatusEntry(
                Status.POLICY_VALIDATED,
                "2024-03-15T08:30:00Z",
                "Policy active and covers the claimed event. Processing continues.",
                "POLICY_SERVICE"
            )
        ));

        ClaimStatus claim2 = new ClaimStatus(
            "CLM-2024-002",
            Status.POLICY_VALIDATED,
            history2,
            "2024-03-15T08:30:00Z",
            "2024-03-25T00:00:00Z"
        );

        store.put(claim1.getClaimId(), claim1);
        store.put(claim2.getClaimId(), claim2);
    }

    public Optional<ClaimStatus> findByClaimId(String claimId) {
        return Optional.ofNullable(store.get(claimId));
    }

    public List<ClaimStatus> findByCurrentStatus(Status status) {
        return store.values().stream()
            .filter(cs -> status.equals(cs.getCurrentStatus()))
            .collect(Collectors.toList());
    }

    public List<ClaimStatus> findAll() {
        return new ArrayList<>(store.values());
    }

    public synchronized ClaimStatus save(ClaimStatus claimStatus) {
        store.put(claimStatus.getClaimId(), claimStatus);
        return claimStatus;
    }

    public boolean exists(String claimId) {
        return store.containsKey(claimId);
    }
}
