package com.pharoah.orchestrator;

import com.pharoah.core.enums.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// VARIJ: User-defined agent configuration and deployment blueprint.
// Specifies responsibilities, agent kind, sub-topics, priority, execution state, and thread binding.
public class AgentDeployment {

    private final String id;
    private String name;
    private String role;
    private String responsibility;
    private AgentKind kind;
    private List<String> subTopics;
    private Priority priority;
    private final List<String> dependencies;

    private volatile AgentStatus status;
    private volatile String assignedThreadName;
    private volatile long executionDurationMs;
    private volatile String outputSnippet;

    public AgentDeployment(String id, String name, AgentKind kind, String responsibility, Priority priority, List<String> dependencies) {
        this.id = id;
        this.name = name;
        this.kind = (kind != null) ? kind : AgentKind.RESEARCH_PAPER;
        this.role = this.kind.name();
        this.responsibility = responsibility;
        this.subTopics = this.kind.generateSubTopics(responsibility);
        this.priority = (priority != null) ? priority : Priority.MEDIUM;
        this.dependencies = new ArrayList<>(dependencies != null ? dependencies : Collections.emptyList());
        this.status = this.dependencies.isEmpty() ? AgentStatus.READY : AgentStatus.WAITING;
        this.assignedThreadName = "Unassigned";
        this.executionDurationMs = 0;
        this.outputSnippet = "";
    }

    public AgentDeployment(String id, String name, AgentKind kind, String responsibility, Priority priority) {
        this(id, name, kind, responsibility, priority, Collections.emptyList());
    }

    // Backwards-compatible constructor
    public AgentDeployment(String id, String name, String role, String responsibility, Priority priority, List<String> dependencies) {
        this(id, name, parseKind(role), responsibility, priority, dependencies);
    }

    public AgentDeployment(String id, String name, String role, String responsibility, Priority priority) {
        this(id, name, parseKind(role), responsibility, priority, Collections.emptyList());
    }

    private static AgentKind parseKind(String role) {
        if (role == null) return AgentKind.RESEARCH_PAPER;
        String r = role.toUpperCase();
        if (r.contains("DATA") || r.contains("ENQUIRY") || r.contains("METRIC")) return AgentKind.DATA_ENQUIRY;
        if (r.contains("CREATIVE") || r.contains("PLOT") || r.contains("CHARACTER") || r.contains("SCENE")) return AgentKind.CREATIVE;
        if (r.contains("ARCHITECT") || r.contains("SOFTWARE") || r.contains("API") || r.contains("TECH")) return AgentKind.TECHNICAL_ARCHITECTURE;
        if (r.contains("STRATEGIC") || r.contains("BUSINESS") || r.contains("MARKET") || r.contains("RISK")) return AgentKind.STRATEGIC_ANALYSIS;
        return AgentKind.RESEARCH_PAPER;
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

    public AgentKind getKind() {
        return kind;
    }

    public void setKind(AgentKind kind) {
        this.kind = kind;
        this.role = (kind != null) ? kind.name() : "SPECIALIST";
        if (this.responsibility != null && (this.subTopics == null || this.subTopics.isEmpty())) {
            this.subTopics = kind.generateSubTopics(this.responsibility);
        }
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
        if (this.kind != null) {
            this.subTopics = this.kind.generateSubTopics(responsibility);
        }
    }

    public List<String> getSubTopics() {
        if (subTopics == null || subTopics.isEmpty()) {
            if (kind != null && responsibility != null) {
                subTopics = kind.generateSubTopics(responsibility);
            } else {
                subTopics = new ArrayList<>();
            }
        }
        return Collections.unmodifiableList(subTopics);
    }

    public void setSubTopics(List<String> subTopics) {
        this.subTopics = new ArrayList<>(subTopics != null ? subTopics : Collections.emptyList());
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

    public void clearDependencies() {
        dependencies.clear();
        if (status == AgentStatus.WAITING) {
            status = AgentStatus.READY;
        }
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
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
}
