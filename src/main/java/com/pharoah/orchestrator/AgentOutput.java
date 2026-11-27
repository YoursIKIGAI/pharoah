package com.pharoah.orchestrator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// VARIJ: Thread-safe data structure holding an individual agent's completed work.
// Facilitates inter-agent communication across dependent workflow stages.
public class AgentOutput {

    private final String agentId;
    private final String agentName;
    private final String responsibility;
    private final String content;
    private final int wordCount;
    private final List<String> citations;
    private final long durationMs;
    private final String threadName;
    private final LocalDateTime completedAt;
    private final boolean success;
    private final String errorMessage;

    public AgentOutput(String agentId, String agentName, String responsibility, String content,
                       List<String> citations, long durationMs, String threadName) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.responsibility = responsibility;
        this.content = (content != null) ? content : "";
        this.wordCount = this.content.isBlank() ? 0 : this.content.split("\\s+").length;
        this.citations = new ArrayList<>(citations != null ? citations : Collections.emptyList());
        this.durationMs = durationMs;
        this.threadName = (threadName != null) ? threadName : Thread.currentThread().getName();
        this.completedAt = LocalDateTime.now();
        this.success = true;
        this.errorMessage = null;
    }

    public static AgentOutput failure(String agentId, String agentName, String responsibility, String error, long durationMs) {
        String msg = (error != null) ? error : "Unknown error";
        return new AgentOutput(agentId, agentName, responsibility, msg, Collections.emptyList(), durationMs, Thread.currentThread().getName(), false, msg);
    }

    private AgentOutput(String agentId, String agentName, String responsibility, String content,
                        List<String> citations, long durationMs, String threadName, boolean success, String errorMessage) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.responsibility = responsibility;
        this.content = (content != null) ? content : "";
        this.wordCount = this.content.isBlank() ? 0 : this.content.split("\\s+").length;
        this.citations = citations;
        this.durationMs = durationMs;
        this.threadName = threadName;
        this.completedAt = LocalDateTime.now();
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getResponsibility() {
        return responsibility;
    }

    public String getContent() {
        return content;
    }

    public int getWordCount() {
        return wordCount;
    }

    public List<String> getCitations() {
        return Collections.unmodifiableList(citations);
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getThreadName() {
        return threadName;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return String.format("[%s on %s] %d words in %d ms (success=%b)", agentName, threadName, wordCount, durationMs, success);
    }
}
