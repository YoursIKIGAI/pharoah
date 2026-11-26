package com.pharoah.orchestrator;

import com.pharoah.core.enums.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// VARIJ: User-defined agent configuration and deployment blueprint.
// Specifies responsibilities, priority, prerequisites, execution state, and thread binding.
public class AgentDeployment {

    private final String id;
    private String name;
    private String role;
    private String responsibility;
    private Priority priority;
    private final List<String> dependencies;

    private volatile AgentStatus status;
    private volatile String assignedThreadName;
    private volatile long executionDurationMs;
    private volatile String outputSnippet;

    public AgentDeployment(String id, String name, String role, String responsibility, Priority priority, List<String> dependencies) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.responsibility = responsibility;
        this.priority = (priority != null) ? priority : Priority.MEDIUM;
        this.dependencies = new ArrayList<>(dependencies != null ? dependencies : Collections.emptyList());
        this.status = this.dependencies.isEmpty() ? AgentStatus.READY : AgentStatus.WAITING;
        this.assignedThreadName = "Unassigned";
        this.executionDurationMs = 0;
        this.outputSnippet = "";
    }

    public AgentDeployment(String id, String name, String role, String responsibility, Priority priority) {
        this(id, name, role, responsibility, priority, Collections.emptyList());
    }

    public boolean isIndependent() {
        return dependencies.isEmpty();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getResponsibility() {
        return responsibility;
    }

    public void setResponsibility(String responsibility) {
        this.responsibility = responsibility;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addDependency(String prerequisiteId) {
        if (prerequisiteId != null && !prerequisiteId.equals(this.id) && !dependencies.contains(prerequisiteId)) {
            dependencies.add(prerequisiteId);
            if (status == AgentStatus.READY) {
                status = AgentStatus.WAITING;
            }
        }
    }

    public void removeDependency(String prerequisiteId) {
        dependencies.remove(prerequisiteId);
        if (dependencies.isEmpty() && status == AgentStatus.WAITING) {
            status = AgentStatus.READY;
        }
    }

    public void clearDependencies() {
        dependencies.clear();
        if (status == AgentStatus.WAITING) {
            status = AgentStatus.READY;
        }
    }

    public AgentStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getAssignedThreadName() {
        return assignedThreadName;
    }

    public void setAssignedThreadName(String assignedThreadName) {
        this.assignedThreadName = assignedThreadName;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public String getOutputSnippet() {
        return outputSnippet;
    }

    public void setOutputSnippet(String outputSnippet) {
        this.outputSnippet = outputSnippet;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - Priority: %s, Prereqs: %s",
                id, name, responsibility, priority, dependencies.isEmpty() ? "None (Independent)" : dependencies);
    }
}
