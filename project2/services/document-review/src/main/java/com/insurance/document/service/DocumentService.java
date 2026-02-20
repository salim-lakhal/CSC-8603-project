package com.insurance.document.service;

import com.insurance.document.model.Document;
import com.insurance.document.model.DocumentStatus;
import com.insurance.document.model.DocumentType;
import com.insurance.document.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document getDocument(String id) {
        return documentRepository.findById(id).orElse(null);
    }

    public List<Document> getDocumentsByClaimId(String claimId) {
        return documentRepository.findByClaimId(claimId);
    }

    public List<Document> getPendingDocuments() {
        return documentRepository.findPending();
    }

    public Document submitDocument(String claimId, DocumentType documentType, String fileName) {
        // POLICE_REPORT and MEDICAL_RECORD are pre-validated; others require adjuster review
        boolean autoValid = isAutoValidated(documentType);

        Document document = new Document(
            UUID.randomUUID().toString(),
            claimId,
            documentType,
            fileName,
            DocumentStatus.PENDING_REVIEW,
            null,
            null,
            autoValid
        );

        return documentRepository.save(document);
    }

    public Document reviewDocument(String id, DocumentStatus status, String reviewNotes) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "Document not found with id: " + id));

        document.setStatus(status);
        document.setReviewNotes(reviewNotes);
        document.setReviewedAt(Instant.now().toString());

        // A document is considered valid only when explicitly approved
        document.setValid(DocumentStatus.APPROVED.equals(status));

        return documentRepository.save(document);
    }

    private boolean isAutoValidated(DocumentType documentType) {
        return documentType == DocumentType.POLICE_REPORT
            || documentType == DocumentType.MEDICAL_RECORD;
    }
}
