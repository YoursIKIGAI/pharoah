package com.pharoah.orchestrator;

// VARIJ: Execution lifecycle states for multi-agent swarm deployment.
public enum AgentStatus {
    WAITING("WAITING", "[!] WAITING"),
    READY("READY", "[▶] READY"),
    RUNNING("RUNNING", "[⏳] RUNNING"),
    COMPLETED("COMPLETED", "[✓] COMPLETED"),
    FAILED("FAILED", "[✗] FAILED"),
    SKIPPED("SKIPPED", "[-] SKIPPED");

    private final String displayName;
    private final String badge;

    AgentStatus(String displayName, String badge) {
        this.displayName = displayName;
        this.badge = badge;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadge() {
        return badge;
    }
}
