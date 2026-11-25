package com.pharoah.document;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// SANCHIT: Implemented Document entity.
// Represents a multi-section document (Research Paper, Blog, Tech Doc, Proposal) tied to a project.
public class Document {

    private final String documentId;
    private final String projectId;
    private String title;
    private DocumentType documentType;
    private String status; // DRAFT, IN_PROGRESS, COMPILED
    private final LocalDateTime createdAt;

    public Document(String documentId, String projectId, String title, DocumentType documentType, String status, LocalDateTime createdAt) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Document title cannot be empty.");
        }
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentType cannot be null.");
        }
        this.documentId = (documentId != null && !documentId.trim().isEmpty()) ? documentId : UUID.randomUUID().toString();
        this.projectId = (projectId != null && !projectId.trim().isEmpty()) ? projectId : "GLOBAL";
        this.title = title.trim();
        this.documentType = documentType;
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "DRAFT";
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public Document(String projectId, String title, DocumentType documentType) {
        this(UUID.randomUUID().toString(), projectId, title, documentType, "DRAFT", LocalDateTime.now());
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document document)) return false;
        return Objects.equals(documentId, document.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %-25s | Format: %-22s | Status: %s",
                documentId.substring(0, Math.min(8, documentId.length())),
                title,
                documentType.getDisplayName(),
                status);
    }
}
