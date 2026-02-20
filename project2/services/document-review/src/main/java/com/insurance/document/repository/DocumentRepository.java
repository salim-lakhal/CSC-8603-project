package com.insurance.document.repository;

import com.insurance.document.model.Document;
import com.insurance.document.model.DocumentStatus;
import com.insurance.document.model.DocumentType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class DocumentRepository {

    private final Map<String, Document> store = new ConcurrentHashMap<>();

    public DocumentRepository() {
        seedSampleData();
    }

    private void seedSampleData() {
        Document doc1 = new Document(
            "doc-001",
            "CLM-2024-001",
            DocumentType.POLICE_REPORT,
            "police_report_CLM2024001.pdf",
            DocumentStatus.APPROVED,
            "Complete report with case number and officer signature. Accepted.",
            "2024-03-15T10:30:00Z",
            true
        );

        Document doc2 = new Document(
            "doc-002",
            "CLM-2024-001",
            DocumentType.REPAIR_ESTIMATE,
            "repair_estimate_garage_dupont.pdf",
            DocumentStatus.PENDING_REVIEW,
            null,
            null,
            false
        );

        Document doc3 = new Document(
            "doc-003",
            "CLM-2024-002",
            DocumentType.MEDICAL_RECORD,
            "medical_record_hopital_nord.pdf",
            DocumentStatus.PENDING_REVIEW,
            null,
            null,
            true
        );

        store.put(doc1.getId(), doc1);
        store.put(doc2.getId(), doc2);
        store.put(doc3.getId(), doc3);
    }

    public Optional<Document> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Document> findByClaimId(String claimId) {
        return store.values().stream()
            .filter(doc -> claimId.equals(doc.getClaimId()))
            .collect(Collectors.toList());
    }

    public List<Document> findPending() {
        return store.values().stream()
            .filter(doc -> DocumentStatus.PENDING_REVIEW.equals(doc.getStatus()))
            .collect(Collectors.toList());
    }

    public List<Document> findAll() {
        return new ArrayList<>(store.values());
    }

    public synchronized Document save(Document document) {
        store.put(document.getId(), document);
        return document;
    }
}
