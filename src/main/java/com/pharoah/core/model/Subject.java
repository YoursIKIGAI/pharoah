package com.pharoah.core.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an isolated Subject Knowledge Space.
 */
public class Subject {

    private final String subjectId;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;

    public Subject(String subjectId, String name, String description, LocalDateTime createdAt) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name cannot be null or empty.");
        }
        this.subjectId = (subjectId != null) ? subjectId : UUID.randomUUID().toString();
        this.name = name.trim();
        this.description = (description != null) ? description.trim() : "";
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public Subject(String name, String description) {
        this(UUID.randomUUID().toString(), name, description, LocalDateTime.now());
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject subject)) return false;
        return Objects.equals(subjectId, subject.subjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectId);
    }

    @Override
    public String toString() {
        return String.format("Subject[id=%s, name='%s', desc='%s']",
                subjectId.substring(0, 8), name, description);
    }
}
