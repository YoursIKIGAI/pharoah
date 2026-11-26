package com.pharoah.bridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// VARIJ: Implemented PythonAgentBridge.
// Bridges the Java application and terminal interface with the Python Multi-Agent Swarm.
// Launches Python subprocess via ProcessBuilder and streams live agent execution logs in real time.
public class PythonAgentBridge {

    private final String pythonExecutable;
    private final File workingDirectory;

    public PythonAgentBridge() {
        this.workingDirectory = new File(".").getAbsoluteFile();
        this.pythonExecutable = detectPythonExecutable();
    }

    public PythonAgentBridge(String customPythonPath) {
        this.pythonExecutable = customPythonPath;
        this.workingDirectory = new File(".").getAbsoluteFile();
    }

    // VARIJ: Detects available python executable on host OS
    private String detectPythonExecutable() {
        String[] candidates = {"python", "python3", "py"};
        for (String c : candidates) {
            try {
                Process p = new ProcessBuilder(c, "--version").start();
                int code = p.waitFor();
                if (code == 0) {
                    return c;
                }
            } catch (Exception ignored) {
            }
        }
        return "python";
    }

    // VARIJ: Executes Python Multi-Agent Swarm with real-time output streaming
    public PythonExecutionResult runWorkflow(String workflow,
                                             String topic,
                                             String sections,
                                             String priorities,
                                             String agents) {

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add("python/main.py");
        command.add("--workflow");
        command.add(workflow != null ? workflow : "1");
        command.add("--topic");
        command.add(topic != null ? topic : "General Research");
        command.add("--sections");
        command.add(sections != null && !sections.isBlank() ? sections : "ALL");

        if (priorities != null && !priorities.isBlank()) {
            command.add("--priorities");
            command.add(priorities);
        }

        if (agents != null && !agents.isBlank()) {
            command.add("--agents");
            command.add(agents);
        }

        List<String> outputLines = new ArrayList<>();
        StringBuilder jsonBuilder = new StringBuilder();
        boolean capturingJson = false;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDirectory);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("__PHAROAH_JSON_RESULT_START__")) {
                        capturingJson = true;
                        continue;
                    }
                    if (line.contains("__PHAROAH_JSON_RESULT_END__")) {
                        capturingJson = false;
                        continue;
                    }

                    if (capturingJson) {
                        jsonBuilder.append(line);
                    } else {
                        // Stream output to terminal in real time
                        System.out.println(line);
                        outputLines.add(line);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("[BRIDGE WARN] Python agent swarm process exited with code: " + exitCode);
                return new PythonExecutionResult(false, "FAILED", topic, workflow, List.of(), List.of(), "", 0.0);
            }

            return parseJsonResult(jsonBuilder.toString(), topic, workflow);

        } catch (Exception e) {
            System.err.println("[BRIDGE ERROR] Failed to execute Python agent swarm: " + e.getMessage());
            return new PythonExecutionResult(false, "ERROR", topic, workflow, List.of(), List.of(), "", 0.0);
        }
    }

    // VARIJ: Parses the JSON summary payload produced at the end of Python execution
    private PythonExecutionResult parseJsonResult(String rawJson, String fallbackTopic, String fallbackWorkflow) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return new PythonExecutionResult(true, "COMPLETED", fallbackTopic, fallbackWorkflow,
                    List.of("All"), List.of("Swarm Agents"), "", 0.0);
        }

        try {
            // Simple robust key extraction without heavy external JSON dependency
            String status = extractField(rawJson, "status", "COMPLETED");
            String topic = extractField(rawJson, "topic", fallbackTopic);
            String workflow = extractField(rawJson, "workflow", fallbackWorkflow);
            String outputFile = extractField(rawJson, "output_file", "");

            double elapsed = 0.0;
            String elapsedStr = extractField(rawJson, "elapsed_seconds", "0.0");
            try {
                elapsed = Double.parseDouble(elapsedStr);
            } catch (Exception ignored) {}

            List<String> sections = extractList(rawJson, "sections");
            List<String> agents = extractList(rawJson, "agents");

            return new PythonExecutionResult(true, status, topic, workflow, sections, agents, outputFile, elapsed);
        } catch (Exception e) {
            return new PythonExecutionResult(true, "COMPLETED", fallbackTopic, fallbackWorkflow,
                    List.of(), List.of(), "", 0.0);
        }
    }

    private String extractField(String json, String field, String defaultValue) {
        String pattern = "\"" + field + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return defaultValue;
        int start = idx + pattern.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        char endChar = (json.charAt(idx + pattern.length()) == ' ' && json.indexOf('"', start - 1) == start - 1) ? '"' : ',';
        if (json.charAt(start - 1) == '"') {
            end = json.indexOf('"', start);
        } else {
            end = json.indexOf(',', start);
            if (end == -1) end = json.indexOf('}', start);
        }
        if (end == -1) return defaultValue;
        return json.substring(start, end).trim().replace("\\\\", "\\");
    }

    private List<String> extractList(String json, String field) {
        List<String> list = new ArrayList<>();
        String pattern = "\"" + field + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return list;
        int start = json.indexOf('[', idx);
        int end = json.indexOf(']', start);
        if (start != -1 && end != -1) {
            String sub = json.substring(start + 1, end);
            String[] tokens = sub.split(",");
            for (String t : tokens) {
                String clean = t.trim().replaceAll("^\"|\"$", "").trim();
                if (!clean.isEmpty()) {
                    list.add(clean);
                }
            }
        }
        return list;
    }
}
