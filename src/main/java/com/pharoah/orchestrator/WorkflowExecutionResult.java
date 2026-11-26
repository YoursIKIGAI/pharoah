package com.pharoah.orchestrator;

import java.util.Collections;
import java.util.List;

// VARIJ: Data transfer object encapsulating the completed workflow results.
public class WorkflowExecutionResult {

    private final boolean success;
    private final String topic;
    private final String category;
    private final String outputFilePath;
    private final double durationSeconds;
    private final List<String> agentsUsed;
    private final List<String> sectionsWorked;

    public WorkflowExecutionResult(boolean success,
                                   String topic,
                                   String category,
                                   String outputFilePath,
                                   double durationSeconds,
                                   List<String> agentsUsed,
                                   List<String> sectionsWorked) {
        this.success = success;
        this.topic = topic;
        this.category = category;
        this.outputFilePath = outputFilePath;
        this.durationSeconds = durationSeconds;
        this.agentsUsed = agentsUsed != null ? agentsUsed : Collections.emptyList();
        this.sectionsWorked = sectionsWorked != null ? sectionsWorked : Collections.emptyList();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTopic() {
        return topic;
    }

    public String getCategory() {
        return category;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public List<String> getAgentsUsed() {
        return Collections.unmodifiableList(agentsUsed);
    }

    public List<String> getSectionsWorked() {
        return Collections.unmodifiableList(sectionsWorked);
    }
}
