package com.pharoah.bridge;

import java.util.List;

// VARIJ: Data container representing the result of a Python multi-agent swarm or tool execution.
public class PythonExecutionResult {

    private final boolean success;
    private final String status;
    private final String topic;
    private final String workflow;
    private final List<String> sections;
    private final List<String> agentsUsed;
    private final String outputFile;
    private final double elapsedSeconds;
    private final String rawOutput;

    public PythonExecutionResult(boolean success,
                                 String status,
                                 String topic,
                                 String workflow,
                                 List<String> sections,
                                 List<String> agentsUsed,
                                 String outputFile,
                                 double elapsedSeconds,
                                 String rawOutput) {
        this.success = success;
        this.status = status;
        this.topic = topic;
        this.workflow = workflow;
        this.sections = sections;
        this.agentsUsed = agentsUsed;
        this.outputFile = outputFile;
        this.elapsedSeconds = elapsedSeconds;
        this.rawOutput = rawOutput != null ? rawOutput : "";
    }

    public PythonExecutionResult(boolean success,
                                 String status,
                                 String topic,
                                 String workflow,
                                 List<String> sections,
                                 List<String> agentsUsed,
                                 String outputFile,
                                 double elapsedSeconds) {
        this(success, status, topic, workflow, sections, agentsUsed, outputFile, elapsedSeconds, "");
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatus() {
        return status;
    }

    public String getTopic() {
        return topic;
    }

    public String getWorkflow() {
        return workflow;
    }

    public List<String> getSections() {
        return sections;
    }

    public List<String> getAgentsUsed() {
        return agentsUsed;
    }

    public List<String> getAgents() {
        return agentsUsed;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public String getRawOutput() {
        return rawOutput;
    }

    @Override
    public String toString() {
        return String.format("PythonExecutionResult[status=%s, topic='%s', sections=%d, agents=%s, elapsed=%.2fs]",
                status, topic, sections != null ? sections.size() : 0, agentsUsed, elapsedSeconds);
    }
}
