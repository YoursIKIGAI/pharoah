package com.pharoah.core.model;

import java.time.LocalDateTime;

// SANCHIT: Implemented UserActivity entity.
// Represents a persistent record of user activity in MySQL Workbench.
public class UserActivity {

    private int activityId;
    private final String userName;
    private final String topic;
    private final String workflowType;
    private final String sectionsWorked;
    private final String agentsUsed;
    private final String status;
    private final LocalDateTime createdAt;

    public UserActivity(int activityId, String userName, String topic, String workflowType,
                        String sectionsWorked, String agentsUsed, String status, LocalDateTime createdAt) {
        this.activityId = activityId;
        this.userName = (userName != null && !userName.trim().isEmpty()) ? userName.trim() : "Student";
        this.topic = (topic != null) ? topic.trim() : "General";
        this.workflowType = (workflowType != null) ? workflowType.trim() : "Custom Workflow";
        this.sectionsWorked = (sectionsWorked != null) ? sectionsWorked.trim() : "All";
        this.agentsUsed = (agentsUsed != null) ? agentsUsed.trim() : "Swarm Agents";
        this.status = (status != null) ? status.trim() : "COMPLETED";
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public UserActivity(String userName, String topic, String workflowType, String sectionsWorked, String agentsUsed) {
        this(0, userName, topic, workflowType, sectionsWorked, agentsUsed, "COMPLETED", LocalDateTime.now());
    }

    public int getActivityId() {
        return activityId;
    }

    public String getUserName() {
        return userName;
    }

    public String getTopic() {
        return topic;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public String getSectionsWorked() {
        return sectionsWorked;
    }

    public String getAgentsUsed() {
        return agentsUsed;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("[%d] User: %-10s | Workflow: %-20s | Topic: %-25s | Status: %s",
                activityId, userName, workflowType, topic, status);
    }
}
