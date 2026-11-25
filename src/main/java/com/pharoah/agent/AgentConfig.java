package com.pharoah.agent;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// VARIJ: Implemented AgentConfig domain entity.
// Represents a configurable agent with customizable role, instructions, and AI provider type.
public class AgentConfig {

    private final String agentId;
    private String name;
    private AgentRole role;
    private String instructions;
    private String providerType; // "MOCK_HEURISTIC", "GEMINI_API", "OLLAMA"
    private boolean isActive;
    private final LocalDateTime createdAt;

    public AgentConfig(String agentId, String name, AgentRole role, String instructions, String providerType, boolean isActive, LocalDateTime createdAt) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name cannot be empty.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Agent role cannot be null.");
        }
        this.agentId = (agentId != null && !agentId.trim().isEmpty()) ? agentId : UUID.randomUUID().toString();
        this.name = name.trim();
        this.role = role;
        this.instructions = (instructions != null && !instructions.trim().isEmpty()) ? instructions.trim() : role.getDescription();
        this.providerType = (providerType != null && !providerType.trim().isEmpty()) ? providerType.trim().toUpperCase() : "MOCK_HEURISTIC";
        this.isActive = isActive;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public AgentConfig(String name, AgentRole role, String instructions, String providerType) {
        this(UUID.randomUUID().toString(), name, role, instructions, providerType, true, LocalDateTime.now());
    }

    public AgentConfig(String name, AgentRole role) {
        this(name, role, role.getDescription(), "MOCK_HEURISTIC");
    }

    public String getAgentId() {
        return agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AgentRole getRole() {
        return role;
    }

    public void setRole(AgentRole role) {
        this.role = role;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isMock() {
        return "MOCK_HEURISTIC".equalsIgnoreCase(providerType) || "MOCK".equalsIgnoreCase(providerType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentConfig that)) return false;
        return Objects.equals(agentId, that.agentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId);
    }

    @Override
    public String toString() {
        String badge = isMock() ? "[MOCK]" : "[REAL AI: " + providerType + "]";
        return String.format("[%s] %-20s | Role: %-18s | %s | Active: %b",
                agentId.substring(0, Math.min(8, agentId.length())),
                name,
                role.getDisplayName(),
                badge,
                isActive);
    }
}
