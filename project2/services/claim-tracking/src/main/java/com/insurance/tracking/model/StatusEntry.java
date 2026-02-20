package com.insurance.tracking.model;

public class StatusEntry {

    private Status status;
    private String timestamp;
    private String description;
    private String updatedBy;

    public StatusEntry() {
    }

    public StatusEntry(Status status, String timestamp, String description, String updatedBy) {
        this.status = status;
        this.timestamp = timestamp;
        this.description = description;
        this.updatedBy = updatedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "StatusEntry{" +
               "status=" + status +
               ", timestamp='" + timestamp + '\'' +
               ", updatedBy='" + updatedBy + '\'' +
               '}';
    }
}
