package com.pharoah.cli;

import com.pharoah.core.enums.Priority;
import com.pharoah.orchestrator.*;
import com.pharoah.persistence.dao.UserActivityDAO;

import java.io.File;
import java.util.*;

// VARIJ, ASHUTOSH, SANCHIT: Clean, Streamlined Terminal Interface for PHAROAH.
// Follows the 4-step interaction flow:
// 1. Number of agents
// 2. Assign topic per agent
// 3. Execution mode & priorities
// 4. Concurrency deployment & clean output presentation
public class PharoahCLI {

    private final WorkflowOrchestrator workflowOrchestrator;
    private final Scanner scanner;

    public PharoahCLI(UserActivityDAO userActivityDAO) {
        this.workflowOrchestrator = new WorkflowOrchestrator(userActivityDAO);
        this.scanner = new Scanner(System.in);
    }

    public PharoahCLI() {
        this(null);
    }

    public void startShell() {
        boolean continueRunning = true;

        while (continueRunning) {
            printHeader();

            // STEP 1: SELECT NUMBER OF AGENTS
            int agentCount = promptAgentCount();

            // STEP 2: ASSIGN TOPICS FOR EACH AGENT
            List<String> topics = promptAgentTopics(agentCount);

            // STEP 3: SPECIFY PRIORITIES AND EXECUTION MODE
            List<Priority> priorities = promptAgentPriorities(agentCount);
            ExecutionMode mode = promptExecutionMode();

            // Assemble agent deployment plan
            List<AgentDeployment> deployments = new ArrayList<>();
            for (int i = 0; i < agentCount; i++) {
                String agentId = String.format("agent-%02d", i + 1);
                String agentName = "Agent " + (i + 1);
                String topic = topics.get(i);
                Priority priority = priorities.get(i);
                deployments.add(new AgentDeployment(agentId, agentName, "SPECIALIST", topic, priority));
            }

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

                // DISPLAY COMPLETE CLEAN OUTPUTS
                displayOutputs(result, plan);
            }

            System.out.print("\nRun another workflow? (Y/N) [default: N]: ");
            String another = scanner.nextLine().trim();
            if (!another.equalsIgnoreCase("y") && !another.equalsIgnoreCase("yes")) {
                System.out.println("\nExiting PHAROAH. All persistent data preserved.");
                continueRunning = false;
            }
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

    private List<String> promptAgentTopics(int count) {
        List<String> topics = new ArrayList<>();
        System.out.println();

        // Default topics if user presses enter
        String[] defaultTopics = new String[]{
                "Literature review on artificial intelligence",
                "Advantages and disadvantages of artificial intelligence",
                "Applications of artificial intelligence in healthcare",
                "Future developments in artificial intelligence"
        };

        for (int i = 1; i <= count; i++) {
            String defaultTopic = (i <= defaultTopics.length) ? defaultTopics[i - 1] : ("Specialized analysis for domain " + i);
            System.out.printf("Enter topic for Agent %d [default: %s]:\n> ", i, defaultTopic);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                topics.add(defaultTopic);
            } else {
                topics.add(input);
            }
        }
        return topics;
    }

    private List<Priority> promptAgentPriorities(int count) {
        List<Priority> priorities = new ArrayList<>();
        System.out.println();
        System.out.println("Specify agent priority levels (High, Medium, Low):");

        for (int i = 1; i <= count; i++) {
            Priority defaultPriority = (i % 2 == 1) ? Priority.HIGH : Priority.MEDIUM;
            System.out.printf("  Priority for Agent %d (H/M/L) [default: %s]: ", i, defaultPriority);
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.startsWith("H")) {
                priorities.add(Priority.HIGH);
            } else if (input.startsWith("L")) {
                priorities.add(Priority.LOW);
            } else if (input.startsWith("M")) {
                priorities.add(Priority.MEDIUM);
            } else {
                priorities.add(defaultPriority);
            }
        }
        return priorities;
    }

    private ExecutionMode promptExecutionMode() {
        System.out.println();
        System.out.println("Select execution mode:");
        System.out.println("  1. Parallel");
        System.out.println("  2. Priority-based");
        System.out.print("Choice (1-2) [default: 1]: ");
        String choice = scanner.nextLine().trim();

        if ("2".equals(choice)) {
            return ExecutionMode.PRIORITY_BASED;
        }
        return ExecutionMode.PARALLEL;
    }

    private void displayOutputs(WorkflowExecutionResult result, WorkflowPlan plan) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("GENERATED OUTPUTS");
        System.out.println("==================================================");
        System.out.println();

        Map<String, AgentOutput> outputs = result.getAgentOutputs();

        for (AgentDeployment agent : plan.getAgents()) {
            AgentOutput output = outputs.get(agent.getId());

            System.out.println(agent.getName() + ": " + agent.getResponsibility());
            System.out.println();

            if (output != null && output.getContent() != null && !output.getContent().isBlank()) {
                System.out.println(output.getContent());
            } else {
                System.out.println("No content was generated for this agent.");
            }

            System.out.println();
        }

        System.out.println("==================================================");
        System.out.printf("Total duration: %.2f seconds | Deployed agents: %d\n", result.getDurationSeconds(), plan.getAgents().size());
        File artifact = new File(result.getOutputFilePath());
        System.out.println("Saved output file: " + artifact.getPath());
        System.out.println("==================================================");
    }
}
