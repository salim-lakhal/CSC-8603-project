package com.insurance.document.controller;

import com.insurance.document.model.Document;
import com.insurance.document.model.DocumentStatus;
import com.insurance.document.model.DocumentType;
import com.insurance.document.service.DocumentService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // Queries

    @QueryMapping
    public Document getDocument(@Argument String id) {
        return documentService.getDocument(id);
    }

    @QueryMapping
    public List<Document> getDocumentsByClaimId(@Argument String claimId) {
        return documentService.getDocumentsByClaimId(claimId);
    }

    @QueryMapping
    public List<Document> getPendingDocuments() {
        return documentService.getPendingDocuments();
    }

    // Mutations

    @MutationMapping
    public Document submitDocument(
            @Argument String claimId,
            @Argument DocumentType documentType,
            @Argument String fileName) {
        return documentService.submitDocument(claimId, documentType, fileName);
    }

    @MutationMapping
    public Document reviewDocument(
            @Argument String id,
            @Argument DocumentStatus status,
            @Argument String reviewNotes) {
        return documentService.reviewDocument(id, status, reviewNotes);
    }
}
