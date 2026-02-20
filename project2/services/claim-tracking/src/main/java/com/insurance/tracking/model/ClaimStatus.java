package com.insurance.tracking.model;

import java.util.ArrayList;
import java.util.List;

public class ClaimStatus {

    private String claimId;
    private Status currentStatus;
    private List<StatusEntry> statusHistory;
    private String lastUpdated;
    private String estimatedCompletionDate;

    public ClaimStatus() {
        this.statusHistory = new ArrayList<>();
    }

    public ClaimStatus(String claimId,
                       Status currentStatus,
                       List<StatusEntry> statusHistory,
                       String lastUpdated,
                       String estimatedCompletionDate) {
        this.claimId = claimId;
        this.currentStatus = currentStatus;
        this.statusHistory = statusHistory != null ? statusHistory : new ArrayList<>();
        this.lastUpdated = lastUpdated;
        this.estimatedCompletionDate = estimatedCompletionDate;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public Status getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(Status currentStatus) {
        this.currentStatus = currentStatus;
    }

    public List<StatusEntry> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<StatusEntry> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getEstimatedCompletionDate() {
        return estimatedCompletionDate;
    }

    public void setEstimatedCompletionDate(String estimatedCompletionDate) {
        this.estimatedCompletionDate = estimatedCompletionDate;
    }

    public void addStatusEntry(StatusEntry entry) {
        this.statusHistory.add(entry);
    }

    @Override
    public String toString() {
        return "ClaimStatus{" +
               "claimId='" + claimId + '\'' +
               ", currentStatus=" + currentStatus +
               ", historySize=" + statusHistory.size() +
               '}';
    }
}
