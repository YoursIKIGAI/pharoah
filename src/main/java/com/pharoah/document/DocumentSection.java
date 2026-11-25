package com.pharoah.document;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// SANCHIT & VARIJ: Implemented DocumentSection entity.
// Supports section-by-section independent drafting, agent assignment, review, and versioning.
public class DocumentSection {

    private final String sectionId;
    private final String documentId;
    private final String sectionName;
    private final int sectionOrder;
    private String assignedAgentId;
    private String content;
    private String status; // PENDING, DRAFTED, UNDER_REVIEW, APPROVED
    private int version;
    private LocalDateTime updatedAt;

    public DocumentSection(String sectionId, String documentId, String sectionName, int sectionOrder, String assignedAgentId, String content, String status, int version, LocalDateTime updatedAt) {
        if (sectionName == null || sectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Section name cannot be empty.");
        }
        this.sectionId = (sectionId != null && !sectionId.trim().isEmpty()) ? sectionId : UUID.randomUUID().toString();
        this.documentId = documentId;
        this.sectionName = sectionName.trim();
        this.sectionOrder = sectionOrder;
        this.assignedAgentId = assignedAgentId;
        this.content = (content != null) ? content : "";
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "PENDING";
        this.version = Math.max(1, version);
        this.updatedAt = (updatedAt != null) ? updatedAt : LocalDateTime.now();
    }

    public DocumentSection(String documentId, String sectionName, int sectionOrder) {
        this(UUID.randomUUID().toString(), documentId, sectionName, sectionOrder, null, "", "PENDING", 1, LocalDateTime.now());
    }

    public String getSectionId() {
        return sectionId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public int getSectionOrder() {
        return sectionOrder;
    }

    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public String getContent() {
        return content;
    }

    public void updateContent(String newContent) {
        this.content = newContent;
        this.version++;
        this.updatedAt = LocalDateTime.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public int getVersion() {
        return version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentSection that)) return false;
        return Objects.equals(sectionId, that.sectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionId);
    }

    @Override
    public String toString() {
        return String.format("  [%d] %-24s | Status: %-12s | Agent: %-10s | v%d",
                sectionOrder,
                sectionName,
                status,
                (assignedAgentId != null ? assignedAgentId.substring(0, Math.min(8, assignedAgentId.length())) : "UNASSIGNED"),
                version);
    }
}
