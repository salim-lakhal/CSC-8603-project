package com.insurance.document.model;

public class Document {

    private String id;
    private String claimId;
    private DocumentType documentType;
    private String fileName;
    private DocumentStatus status;
    private String reviewNotes;
    // null until an adjuster reviews the document
    private String reviewedAt;
    private boolean valid;

    public Document() {
    }

    public Document(String id,
                    String claimId,
                    DocumentType documentType,
                    String fileName,
                    DocumentStatus status,
                    String reviewNotes,
                    String reviewedAt,
                    boolean valid) {
        this.id = id;
        this.claimId = claimId;
        this.documentType = documentType;
        this.fileName = fileName;
        this.status = status;
        this.reviewNotes = reviewNotes;
        this.reviewedAt = reviewedAt;
        this.valid = valid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "Document{" +
               "id='" + id + '\'' +
               ", claimId='" + claimId + '\'' +
               ", documentType=" + documentType +
               ", fileName='" + fileName + '\'' +
               ", status=" + status +
               ", valid=" + valid +
               '}';
    }
}
