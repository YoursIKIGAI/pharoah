package com.pharoah.knowledge;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// SANCHIT: Implemented KnowledgeResource entity.
// Represents a persistent knowledge asset (URL, research paper, code snippet, note) tied to a project and topic.
public class KnowledgeResource {

    private final String resourceId;
    private final String projectId;
    private String title;
    private ResourceType resourceType;
    private String contentOrUrl;
    private String topicTag;
    private final LocalDateTime createdAt;

    public KnowledgeResource(String resourceId, String projectId, String title, ResourceType resourceType, String contentOrUrl, String topicTag, LocalDateTime createdAt) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource title cannot be empty.");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("ResourceType cannot be null.");
        }
        this.resourceId = (resourceId != null && !resourceId.trim().isEmpty()) ? resourceId : UUID.randomUUID().toString();
        this.projectId = (projectId != null && !projectId.trim().isEmpty()) ? projectId : "GLOBAL";
        this.title = title.trim();
        this.resourceType = resourceType;
        this.contentOrUrl = (contentOrUrl != null) ? contentOrUrl.trim() : "";
        this.topicTag = (topicTag != null && !topicTag.trim().isEmpty()) ? topicTag.trim().toUpperCase() : "GENERAL";
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public KnowledgeResource(String projectId, String title, ResourceType resourceType, String contentOrUrl, String topicTag) {
        this(UUID.randomUUID().toString(), projectId, title, resourceType, contentOrUrl, topicTag, LocalDateTime.now());
    }

    public String getResourceId() {
        return resourceId;
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

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getContentOrUrl() {
        return contentOrUrl;
    }

    public void setContentOrUrl(String contentOrUrl) {
        this.contentOrUrl = contentOrUrl;
    }

    public String getTopicTag() {
        return topicTag;
    }

    public void setTopicTag(String topicTag) {
        this.topicTag = topicTag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KnowledgeResource that)) return false;
        return Objects.equals(resourceId, that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %-25s | Type: %-12s | Topic: %-10s | %s",
                resourceId.substring(0, Math.min(8, resourceId.length())),
                title,
                resourceType,
                topicTag,
                contentOrUrl.length() > 50 ? contentOrUrl.substring(0, 47) + "..." : contentOrUrl);
    }
}
