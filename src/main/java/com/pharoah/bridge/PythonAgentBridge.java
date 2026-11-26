package com.pharoah.bridge;

import com.pharoah.config.AppConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// VARIJ: Implemented PythonAgentBridge.
// Bridges the Java application and terminal interface with the Python Multi-Agent Swarm.
// Launches Python subprocess via ProcessBuilder and streams live agent execution logs in real time.
public class PythonAgentBridge {

    private final String pythonExecutable;
    private final File workingDirectory;

    public PythonAgentBridge() {
        this.workingDirectory = new File(".").getAbsoluteFile();
        String configured = AppConfig.getInstance().getPythonCommand();
        if (configured != null && !configured.isBlank() && !configured.equalsIgnoreCase("python")) {
            this.pythonExecutable = configured;
        } else {
            this.pythonExecutable = detectPythonExecutable();
        }
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
            } catch (Exception ignored) {}
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
                        // Stream live output to terminal
                        System.out.println(line);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("  [!] Swarm process finished with non-zero exit code: " + exitCode);
                return new PythonExecutionResult(false, "FAILED", topic, workflow, List.of(), List.of(), "", 0.0);
            }

            return parseJsonResult(jsonBuilder.toString(), topic, workflow);

        } catch (Exception e) {
            System.err.println("  [!] Failed to execute Python agent swarm: " + e.getMessage());
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
            String status = extractRegex(rawJson, "\"status\"\\s*:\\s*\"([^\"]+)\"", "COMPLETED");
            String topic = extractRegex(rawJson, "\"topic\"\\s*:\\s*\"([^\"]+)\"", fallbackTopic);
            String workflow = extractRegex(rawJson, "\"workflow\"\\s*:\\s*\"([^\"]+)\"", fallbackWorkflow);
            String outputFile = extractRegex(rawJson, "\"output_file\"\\s*:\\s*\"([^\"]+)\"", "");
            String elapsedStr = extractRegex(rawJson, "\"elapsed_seconds\"\\s*:\\s*([0-9.]+)", "0.0");

            double elapsed = 0.0;
            try {
                elapsed = Double.parseDouble(elapsedStr);
            } catch (Exception ignored) {}

            List<String> sections = extractArrayRegex(rawJson, "sections");
            List<String> agents = extractArrayRegex(rawJson, "agents");

            return new PythonExecutionResult(true, status, topic, workflow, sections, agents, outputFile, elapsed);
        } catch (Exception e) {
            return new PythonExecutionResult(true, "COMPLETED", fallbackTopic, fallbackWorkflow,
                    List.of(), List.of(), "", 0.0);
        }
    }

    private String extractRegex(String json, String regex, String defaultValue) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) {
            return m.group(1).replace("\\\\", "\\");
        }
        return defaultValue;
    }

    private List<String> extractArrayRegex(String json, String key) {
        List<String> list = new ArrayList<>();
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher m = p.matcher(json);
        if (m.find()) {
            String content = m.group(1);
            Matcher itemMatcher = Pattern.compile("\"([^\"]+)\"").matcher(content);
            while (itemMatcher.find()) {
                list.add(itemMatcher.group(1).trim());
            }
        }
        return list;
    }
}
