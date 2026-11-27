package com.pharoah.bridge;

import com.pharoah.config.AppConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

// VARIJ & ASHUTOSH: Robust ProcessBuilder Bridge to Python Multi-Agent Swarm.
// Resolves python/main.py from project root, enforces UTF-8 unbuffered subprocess execution,
// drains stderr on a dedicated thread, implements hard timeout process termination,
// and parses the JSON outcome with RFC 8259 SimpleJsonParser without regex.
public class PythonAgentBridge {

    private final String pythonExecutable;
    private final File projectRoot;
    private final long timeoutSeconds;

    public PythonAgentBridge(String customPythonPath, long timeoutSeconds) {
        this.projectRoot = resolveProjectRoot();
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 300L;

        if (customPythonPath != null && !customPythonPath.isBlank()) {
            this.pythonExecutable = customPythonPath;
        } else {
            String configured = AppConfig.getInstance().getPythonCommand();
            if (configured != null && !configured.isBlank() && !configured.equalsIgnoreCase("python")) {
                this.pythonExecutable = configured;
            } else {
                this.pythonExecutable = detectPythonExecutable();
            }
        }
    }

    public PythonAgentBridge(long timeoutSeconds) {
        this(null, timeoutSeconds);
    }

    public PythonAgentBridge() {
        this(null, 300L);
    }

    public PythonAgentBridge(String customPythonPath) {
        this(customPythonPath, 300L);
    }

    // Resolves project root by verifying python/main.py existence upwards
    private static File resolveProjectRoot() {
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File testScript = new File(current, "python" + File.separator + "main.py");
            if (testScript.exists()) {
                return current;
            }
            File pom = new File(current, "pom.xml");
            if (pom.exists() && new File(current, "python").isDirectory()) {
                return current;
            }
            current = current.getParentFile();
        }
        return new File(".").getAbsoluteFile();
    }

    private String detectPythonExecutable() {
        String[] candidates = {"python", "python3", "py"};
        for (String c : candidates) {
            try {
                Process p = new ProcessBuilder(c, "--version").start();
                if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return c;
                }
            } catch (Exception ignored) {}
        }
        return "python";
    }

    public PythonExecutionResult runWorkflow(String workflow,
                                             String topic,
                                             String sections,
                                             String priorities,
                                             String agents) {

        File mainPy = new File(projectRoot, "python" + File.separator + "main.py");
        if (!mainPy.exists()) {
            System.err.println("  [!] Error: python/main.py could not be found at " + mainPy.getAbsolutePath());
            return new PythonExecutionResult(false, "FILE_NOT_FOUND", topic, workflow, List.of(), List.of(), "", 0.0);
        }

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(mainPy.getAbsolutePath());
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

        return executeProcess(command, topic, workflow);
    }

    // Executes process with proper environment, stderr draining, and timeout management
    public PythonExecutionResult executeProcess(List<String> command, String fallbackTopic, String fallbackWorkflow) {
        StringBuilder jsonBuilder = new StringBuilder();
        List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());
        Process process = null;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(projectRoot);
            pb.redirectErrorStream(false); // Separate stderr and stdout streams

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUNBUFFERED", "1");

            process = pb.start();

            // Thread to drain stderr without blocking stdout
            Process finalProcess = process;
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader errReader = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = errReader.readLine()) != null) {
                        stderrLines.add(line);
                        System.err.println("  [PYTHON-STDERR] " + line);
                    }
                } catch (IOException ignored) {}
            }, "Python-StderrDrainer");
            stderrThread.setDaemon(true);
            stderrThread.start();

            // Thread to read stdout and extract structured JSON payload
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean capturingJson = false;
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
                            jsonBuilder.append(line).append("\n");
                        } else {
                            System.out.println(line);
                        }
                    }
                } catch (IOException ignored) {}
            }, "Python-StdoutReader");
            stdoutThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                System.err.println("  [!] Subprocess timed out after " + timeoutSeconds + "s. Destroying process forcibly...");
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                return new PythonExecutionResult(false, "TIMEOUT", fallbackTopic, fallbackWorkflow, List.of(), List.of(), "", 0.0);
            }

            stdoutThread.join(2000);
            stderrThread.join(2000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.err.println("  [!] Python process exited with non-zero code: " + exitCode);
                return new PythonExecutionResult(false, "FAILED", fallbackTopic, fallbackWorkflow, List.of(), List.of(), "", 0.0);
            }

            return parseJsonResult(jsonBuilder.toString().trim(), fallbackTopic, fallbackWorkflow);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("  [!] Execution interrupted. Destroying Python process...");
            if (process != null) {
                process.destroy();
                try {
                    if (!process.waitFor(2, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException ignored) {}
            }
            return new PythonExecutionResult(false, "INTERRUPTED", fallbackTopic, fallbackWorkflow, List.of(), List.of(), "", 0.0);
        } catch (Exception e) {
            System.err.println("  [!] Subprocess launch failure: " + e.getMessage());
            return new PythonExecutionResult(false, "ERROR", fallbackTopic, fallbackWorkflow, List.of(), List.of(), "", 0.0);
        }
    }

    // Parses structured JSON machine payload using RFC 8259 SimpleJsonParser without regex
    private PythonExecutionResult parseJsonResult(String rawJson, String fallbackTopic, String fallbackWorkflow) {
        if (rawJson == null || rawJson.isBlank()) {
            return new PythonExecutionResult(true, "COMPLETED", fallbackTopic, fallbackWorkflow,
                    List.of("General"), List.of("Swarm Agents"), "", 0.0);
        }

        try {
            Map<String, Object> map = SimpleJsonParser.parseObject(rawJson);
            if (map.isEmpty()) {
                return new PythonExecutionResult(true, "COMPLETED", fallbackTopic, fallbackWorkflow,
                        List.of(), List.of(), "", 0.0);
            }

            String status = String.valueOf(map.getOrDefault("status", "COMPLETED"));
            String topic = String.valueOf(map.getOrDefault("topic", fallbackTopic));
            String workflow = String.valueOf(map.getOrDefault("workflow", fallbackWorkflow));
            String outputFile = String.valueOf(map.getOrDefault("output_file", ""));

            double elapsed = 0.0;
            Object elapsedObj = map.get("elapsed_seconds");
            if (elapsedObj instanceof Number n) {
                elapsed = n.doubleValue();
            } else if (elapsedObj != null) {
                try {
                    elapsed = Double.parseDouble(elapsedObj.toString());
                } catch (Exception ignored) {}
            }

            List<String> sections = new ArrayList<>();
            Object secObj = map.get("sections");
            if (secObj instanceof List<?> secList) {
                for (Object item : secList) {
                    if (item != null) sections.add(item.toString());
                }
            }

            List<String> agents = new ArrayList<>();
            Object agObj = map.get("agents");
            if (agObj instanceof List<?> agList) {
                for (Object item : agList) {
                    if (item != null) agents.add(item.toString());
                }
            }

            return new PythonExecutionResult(true, status, topic, workflow, sections, agents, outputFile, elapsed);

        } catch (Exception e) {
            System.err.println("  [!] Failed to parse Python JSON output: " + e.getMessage());
            return new PythonExecutionResult(true, "COMPLETED", fallbackTopic, fallbackWorkflow,
                    List.of(), List.of(), "", 0.0);
        }
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
