package com.pharoah.orchestrator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// VARIJ: Data transfer object encapsulating the completed workflow results and output file paths.
public class WorkflowExecutionResult {

    private final boolean success;
    private final String topic;
    private final String category;
    private final String outputFilePath;
    private final double durationSeconds;
    private final List<String> agentsUsed;
    private final List<String> sectionsWorked;
    private final Map<String, AgentOutput> agentOutputs;
    private final String fullDocument;
    private final Map<String, String> agentFilePaths;

    public WorkflowExecutionResult(boolean success,
                                   String topic,
                                   String category,
                                   String outputFilePath,
                                   double durationSeconds,
                                   List<String> agentsUsed,
                                   List<String> sectionsWorked,
                                   Map<String, AgentOutput> agentOutputs,
                                   String fullDocument,
                                   Map<String, String> agentFilePaths) {
        this.success = success;
        this.topic = topic;
        this.category = category;
        this.outputFilePath = outputFilePath;
        this.durationSeconds = durationSeconds;
        this.agentsUsed = agentsUsed != null ? agentsUsed : Collections.emptyList();
        this.sectionsWorked = sectionsWorked != null ? sectionsWorked : Collections.emptyList();
        this.agentOutputs = agentOutputs != null ? agentOutputs : Collections.emptyMap();
        this.fullDocument = fullDocument != null ? fullDocument : "";
        this.agentFilePaths = agentFilePaths != null ? agentFilePaths : Collections.emptyMap();
    }

    public WorkflowExecutionResult(boolean success,
                                   String topic,
                                   String category,
                                   String outputFilePath,
                                   double durationSeconds,
                                   List<String> agentsUsed,
                                   List<String> sectionsWorked,
                                   Map<String, AgentOutput> agentOutputs,
                                   String fullDocument) {
        this(success, topic, category, outputFilePath, durationSeconds, agentsUsed, sectionsWorked, agentOutputs, fullDocument, Collections.emptyMap());
    }

    public WorkflowExecutionResult(boolean success,
                                   String topic,
                                   String category,
                                   String outputFilePath,
                                   double durationSeconds,
                                   List<String> agentsUsed,
                                   List<String> sectionsWorked) {
        this(success, topic, category, outputFilePath, durationSeconds, agentsUsed, sectionsWorked, Collections.emptyMap(), "", Collections.emptyMap());
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

    public Map<String, AgentOutput> getAgentOutputs() {
        return Collections.unmodifiableMap(agentOutputs);
    }

    public String getFullDocument() {
        return fullDocument;
    }

    public Map<String, String> getAgentFilePaths() {
        return Collections.unmodifiableMap(agentFilePaths);
    }
}
