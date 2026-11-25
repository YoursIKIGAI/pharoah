package com.pharoah.project;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// SANCHIT: Implemented Project entity.
// Represents a persistent project workspace isolating tasks, documents, and knowledge.
public class Project {

    private final String projectId;
    private String name;
    private String description;
    private String status; // ACTIVE, COMPLETED, ARCHIVED
    private final LocalDateTime createdAt;

    public Project(String projectId, String name, String description, String status, LocalDateTime createdAt) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }
        this.projectId = (projectId != null && !projectId.trim().isEmpty()) ? projectId : UUID.randomUUID().toString();
        this.name = name.trim();
        this.description = (description != null) ? description.trim() : "";
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ACTIVE";
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public Project(String projectId, String name, String description) {
        this(projectId, name, description, "ACTIVE", LocalDateTime.now());
    }

    public Project(String name, String description) {
        this(UUID.randomUUID().toString(), name, description, "ACTIVE", LocalDateTime.now());
    }

    public String getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        if (!(o instanceof Project project)) return false;
        return Objects.equals(projectId, project.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Status: %s | Created: %s",
                projectId.substring(0, Math.min(8, projectId.length())),
                name,
                status,
                createdAt.toLocalDate());
    }
}
