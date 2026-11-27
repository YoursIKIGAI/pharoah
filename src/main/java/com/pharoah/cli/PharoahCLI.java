package com.pharoah.cli;

import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.bridge.PythonExecutionResult;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.model.UserActivity;
import com.pharoah.orchestrator.*;
import com.pharoah.persistence.dao.UserActivityDAO;

import java.io.File;
import java.util.*;

// VARIJ, ASHUTOSH, SANCHIT: Clean, Streamlined Terminal Interface for PHAROAH.
// Supports both Native Java Swarm Orchestration and Python Swarm via ProcessBuilder Bridge.
// Automatically records execution audit logs into MySQL user_activity table.
public class PharoahCLI {

    private final WorkflowOrchestrator workflowOrchestrator;
    private final UserActivityDAO userActivityDAO;
    private final PythonAgentBridge pythonBridge;
    private final Scanner scanner;

    public PharoahCLI(UserActivityDAO userActivityDAO) {
        this.userActivityDAO = userActivityDAO;
        this.workflowOrchestrator = new WorkflowOrchestrator(userActivityDAO);
        this.pythonBridge = new PythonAgentBridge();
        this.scanner = new Scanner(System.in);
    }

    public PharoahCLI() {
        this(null);
    }

    public void startShell() {
        boolean continueRunning = true;

        while (continueRunning) {
            printHeader();

            System.out.println("Select Orchestration Engine:");
            System.out.println("  1. Native Java Swarm (User-defined agents, multithreading, priority ready-queue)");
            System.out.println("  2. Python Multi-Agent Swarm (ProcessBuilder Bridge execution)");
            System.out.println("  3. Exit");
            System.out.print("Choice (1-3) [default: 1]: ");
            String mainChoice = scanner.nextLine().trim();

            if ("3".equals(mainChoice)) {
                System.out.println("\nExiting PHAROAH.");
                break;
            } else if ("2".equals(mainChoice)) {
                runPythonSwarmFlow();
            } else {
                runJavaSwarmFlow();
            }

            System.out.print("\nRun another workflow? (Y/N) [default: N]: ");
            String another = scanner.nextLine().trim();
            if (!another.equalsIgnoreCase("y") && !another.equalsIgnoreCase("yes")) {
                System.out.println("\nExiting PHAROAH. All persistent data preserved in outputs/ and database.");
                continueRunning = false;
            }
        }
    }

    private void runJavaSwarmFlow() {
        // STEP 1: SELECT NUMBER OF AGENTS
        int agentCount = promptAgentCount();

        // STEP 2: CONFIGURE AGENT KIND, TOPIC, AND DYNAMIC SUB-TOPICS
        List<AgentDeployment> deployments = promptAgentConfigurations(agentCount);

        // STEP 3: SPECIFY PRIORITIES AND EXECUTION MODE
        promptAgentPriorities(deployments);
        ExecutionMode mode = promptExecutionMode();

        // STEP 4: CONFIRMATION & EXECUTION
        System.out.print("\nDeploy agents? (Y/N) [default: Y]: ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("n") || confirm.equalsIgnoreCase("no")) {
            System.out.println("Workflow deployment cancelled.");
        } else {
            WorkflowPlan plan = new WorkflowPlan(
                    WorkflowCategory.CUSTOM_WORKFLOW,
                    "Multi-Agent Execution",
                    deployments,
                    Math.max(1, agentCount),
                    mode
            );

            WorkflowExecutionResult result = workflowOrchestrator.executeWorkflow(plan);

            // FILE-BASED OUTPUT SUMMARY (Outputs saved to outputs/ folder, not dumped to console)
            displayFileSummary(result, plan);
        }
    }

    private void runPythonSwarmFlow() {
        System.out.println("\n--------------------------------------------------");
        System.out.println("Python Multi-Agent Swarm via Subprocess Bridge");
        System.out.println("--------------------------------------------------");
        System.out.println("Available Workflows:");
        System.out.println("  1. Research Paper");
        System.out.println("  2. Technical Documentation");
        System.out.println("  3. Creative Writing");
        System.out.println("  4. Analytical Report");
        System.out.println("  5. Business Proposal");
        System.out.print("Select Workflow (1-5) [default: 1]: ");
        String wfChoice = scanner.nextLine().trim();
        String wf = wfChoice.isEmpty() ? "1" : wfChoice;

        System.out.print("Enter Topic [default: Impact of Artificial Intelligence on Cybersecurity]: ");
        String topicInput = scanner.nextLine().trim();
        String topic = topicInput.isEmpty() ? "Impact of Artificial Intelligence on Cybersecurity" : topicInput;

        System.out.print("Enter Sections or 'ALL' [default: ALL]: ");
        String sectionsInput = scanner.nextLine().trim();
        String sections = sectionsInput.isEmpty() ? "ALL" : sectionsInput;

        System.out.println("\n[BRIDGE] Launching Python Swarm subprocess...");
        PythonExecutionResult pyResult = pythonBridge.runWorkflow(wf, topic, sections, "", "");

        if (pyResult.isSuccess()) {
            System.out.println("\n==================================================");
            System.out.println("PYTHON SWARM COMPLETED SUCCESSFULLY");
            System.out.println("==================================================");
            System.out.println("Topic        : " + pyResult.getTopic());
            System.out.println("Workflow     : " + pyResult.getWorkflow());
            System.out.println("Output File  : " + pyResult.getOutputFile());
            System.out.printf("Duration     : %.2fs\n", pyResult.getElapsedSeconds());
            System.out.println("==================================================");

            if (userActivityDAO != null) {
                try {
                    String sectionsStr = String.join(", ", pyResult.getSections());
                    String agentsStr = String.join(", ", pyResult.getAgents());
                    UserActivity activity = new UserActivity(
                            "User",
                            pyResult.getTopic(),
                            "Python Swarm (" + pyResult.getWorkflow() + ")",
                            sectionsStr.isEmpty() ? "All" : sectionsStr,
                            agentsStr.isEmpty() ? "Python Swarm" : agentsStr
                    );
                    userActivityDAO.recordActivity(activity);
                    System.out.println("[AUDIT] Recorded Python swarm execution in MySQL user_activity table.");
                } catch (Exception e) {
                    System.err.println("[WARNING] Failed to record Python activity in database: " + e.getMessage());
                }
            }
        } else {
            System.err.println("\n[!] Python swarm execution failed or timed out: " + pyResult.getStatus());
        }
    }

    private void printHeader() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("PHAROAH");
        System.out.println("Multi-Agent Writing System");
        System.out.println("==================================================");
        System.out.println();
    }

    private int promptAgentCount() {
        int count = 4;
        while (true) {
            System.out.print("How many agents do you want to deploy? [default: 4]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return 4;
            }
            try {
                int parsed = Integer.parseInt(input);
                if (parsed >= 1 && parsed <= 16) {
                    return parsed;
                }
                System.out.println("Please enter a number between 1 and 16.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    private List<AgentDeployment> promptAgentConfigurations(int count) {
        List<AgentDeployment> deployments = new ArrayList<>();
        System.out.println();

        String[] defaultTopics = new String[]{
                "Literature review on artificial intelligence",
                "Quantitative data inquiry and empirical benchmarks on artificial intelligence",
                "Creative narrative on artificial intelligence in clinical medicine",
                "Technical architecture and concurrency models for intelligent agents"
        };

        AgentKind[] defaultKinds = new AgentKind[]{
                AgentKind.RESEARCH_PAPER,
                AgentKind.DATA_ENQUIRY,
                AgentKind.CREATIVE,
                AgentKind.TECHNICAL_ARCHITECTURE,
                AgentKind.STRATEGIC_ANALYSIS
        };

        for (int i = 1; i <= count; i++) {
            System.out.println("--------------------------------------------------");
            System.out.printf("Configure Agent %d:\n", i);
            System.out.println("  Select Agent Specialization Kind:");
            System.out.println("    1. Research Paper (Academic foundations, methodology, formal literature)");
            System.out.println("    2. Data Enquiry (Quantitative benchmarks, empirical datasets, error margins)");
            System.out.println("    3. Creative (Narrative scenarios, stakeholder dialogues, field context)");
            System.out.println("    4. Technical Architecture (Component topology, pipelines, concurrency)");
            System.out.println("    5. Strategic Analysis (Advantages, disadvantages, governance, trade-offs)");

            int defaultKindIdx = (i - 1) % defaultKinds.length;
            AgentKind defaultKind = defaultKinds[defaultKindIdx];
            System.out.printf("  Choice (1-5) [default: %d (%s)]: ", defaultKindIdx + 1, defaultKind.getDisplayName());
            String kindChoice = scanner.nextLine().trim();
            AgentKind selectedKind = defaultKind;

            switch (kindChoice) {
                case "1" -> selectedKind = AgentKind.RESEARCH_PAPER;
                case "2" -> selectedKind = AgentKind.DATA_ENQUIRY;
                case "3" -> selectedKind = AgentKind.CREATIVE;
                case "4" -> selectedKind = AgentKind.TECHNICAL_ARCHITECTURE;
                case "5" -> selectedKind = AgentKind.STRATEGIC_ANALYSIS;
            }

            String defaultTopic = (i <= defaultTopics.length) ? defaultTopics[i - 1] : ("Specialized analysis for domain " + i);
            System.out.printf("  Enter topic for Agent %d [default: %s]:\n  > ", i, defaultTopic);
            String inputTopic = scanner.nextLine().trim();
            String topic = inputTopic.isEmpty() ? defaultTopic : inputTopic;

            String agentId = String.format("agent-%02d", i);
            String agentName = "Agent " + i;

            AgentDeployment deployment = new AgentDeployment(agentId, agentName, selectedKind, topic, Priority.MEDIUM);

            // Display dynamic sub-topics formulated for this agent
            List<String> subTopics = deployment.getSubTopics();
            System.out.println("  Dynamically generated sub-topics:");
            for (int s = 0; s < subTopics.size(); s++) {
                System.out.printf("    %d. %s\n", s + 1, subTopics.get(s));
            }
            System.out.println();

            deployments.add(deployment);
        }

        return deployments;
    }

    private void promptAgentPriorities(List<AgentDeployment> deployments) {
        System.out.println("--------------------------------------------------");
        System.out.println("Specify agent priority levels (High, Medium, Low):");

        for (int i = 0; i < deployments.size(); i++) {
            AgentDeployment d = deployments.get(i);
            Priority defaultPriority = (i % 2 == 0) ? Priority.HIGH : Priority.MEDIUM;
            System.out.printf("  Priority for %s (%s) (H/M/L) [default: %s]: ",
                    d.getName(), d.getKind().getDisplayName(), defaultPriority);
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.startsWith("H")) {
                d.setPriority(Priority.HIGH);
            } else if (input.startsWith("L")) {
                d.setPriority(Priority.LOW);
            } else if (input.startsWith("M")) {
                d.setPriority(Priority.MEDIUM);
            } else {
                d.setPriority(defaultPriority);
            }
        }
    }

    private ExecutionMode promptExecutionMode() {
        System.out.println();
        System.out.println("Select execution mode:");
        System.out.println("  1. Parallel (Execute independent agents concurrently)");
        System.out.println("  2. Priority-based (Execute High Priority first, then Medium, then Low)");
        System.out.print("Choice (1-2) [default: 1]: ");
        String choice = scanner.nextLine().trim();

        if ("2".equals(choice)) {
            return ExecutionMode.PRIORITY_BASED;
        }
        return ExecutionMode.PARALLEL;
    }

    // Displays the file-based summary: Outputs are saved to outputs/ folder, NOT printed to the console
    private void displayFileSummary(WorkflowExecutionResult result, WorkflowPlan plan) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("EXECUTION COMPLETE: OUTPUTS PERSISTED");
        System.out.println("==================================================");
        System.out.println("All generated outputs have been saved to the 'outputs' directory.");
        System.out.println("(Console output omitted as requested to prevent terminal clutter.)\n");

        System.out.println("Generated Files:");
        Map<String, String> files = result.getAgentFilePaths();

        for (AgentDeployment agent : plan.getAgents()) {
            String path = files.get(agent.getId());
            if (path != null) {
                System.out.printf("  %s (%s): %s\n", agent.getName(), agent.getKind().getDisplayName(), path);
            }
        }

        System.out.printf("\nConsolidated Document: %s\n", result.getOutputFilePath());
        System.out.println("==================================================");
        System.out.printf("Total duration: %.2f seconds | Deployed agents: %d\n", result.getDurationSeconds(), plan.getAgents().size());
        System.out.println("==================================================");
    }
}
