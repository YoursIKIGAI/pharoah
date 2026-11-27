package com.pharoah.cli;

import com.pharoah.agents.AgentRegistry;
import com.pharoah.llm.LlmClient;
import com.pharoah.orchestrator.*;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.RunDao;

import java.util.List;
import java.util.Scanner;

/**
 * Prompt-driven Command Line REPL Interface for PHAROAH.
 * Accepts free-text inquiries, decomposes tasks via PlannerAgent, executes
 * concurrent multi-agent swarms, and provides REPL commands (/agents, /history, /db, /model, /help).
 */
public class PharoahCLI {

    private final DatabaseConnectionFactory connectionFactory;
    private final RunDao runDao;
    private final LlmClient llmClient;
    private final PlannerAgent plannerAgent;
    private final WorkflowOrchestrator orchestrator;
    private final boolean useTools;
    private final Scanner scanner;
    private final AgentRegistry agentRegistry;

    private WorkflowPlan lastPlan = null;

    public PharoahCLI(DatabaseConnectionFactory connectionFactory,
                      RunDao runDao,
                      LlmClient llmClient,
                      WorkflowOrchestrator orchestrator,
                      boolean useTools) {
        this.connectionFactory = connectionFactory;
        this.runDao = runDao;
        this.llmClient = llmClient;
        this.plannerAgent = new PlannerAgent(llmClient);
        this.orchestrator = orchestrator;
        this.useTools = useTools;
        this.scanner = new Scanner(System.in);
        this.agentRegistry = new AgentRegistry(this.llmClient);
    }

    public void startShell() {
        printBanner();

        while (true) {
            String promptPrefix = llmClient.isMock() ? "pharoah (mock)> " : "pharoah> ";
            System.out.print(promptPrefix);

            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            if (input.equalsIgnoreCase("/exit") || input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting PHAROAH. Persistent audit data preserved in database and outputs/.");
                break;
            }

            if (input.equalsIgnoreCase("/help") || input.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            if (input.equalsIgnoreCase("/agents")) {
                showAgentsDirectory();
                continue;
            }

            if (input.equalsIgnoreCase("/history")) {
                showHistory();
                continue;
            }

            if (input.equalsIgnoreCase("/db")) {
                if (connectionFactory != null) {
                    connectionFactory.printDatabaseStatus();
                } else {
                    System.out.println("Database factory not initialized.");
                }
                continue;
            }

            if (input.equalsIgnoreCase("/model")) {
                System.out.println("\nActive LLM Model: " + llmClient.getModelName() + " (Mock: " + llmClient.isMock() + ")");
                System.out.println();
                continue;
            }

            // Free-text prompt execution
            executeUserPrompt(input);
        }
    }

    public void executeUserPrompt(String userPrompt) {
        System.out.println("\n" + "-".repeat(76));
        System.out.println("Processing prompt: \"" + userPrompt + "\"");
        System.out.println("Generating multi-agent workflow plan via PlannerAgent (" + llmClient.getModelName() + ")...");
        System.out.println("-".repeat(76));

        try {
            WorkflowPlan plan = plannerAgent.plan(userPrompt, 4, ExecutionMode.PARALLEL);
            this.lastPlan = plan;

            System.out.println(">>> PLAN ESTABLISHED: Decomposed into " + plan.getAgents().size() + " specialized agents:");
            for (int i = 0; i < plan.getAgents().size(); i++) {
                AgentDeployment a = plan.getAgents().get(i);
                String deps = a.getDependencies().isEmpty() ? "None (Independent)" : String.join(", ", a.getDependencies());
                System.out.printf("    [%s] %s (%s) | Priority: %s | Depends on: %s\n",
                        a.getId(), a.getName(), a.getRole(), a.getPriority().getDisplayName(), deps);
                System.out.printf("         Task: %s\n", a.getResponsibility());
            }

            // Execute Workflow
            WorkflowExecutionResult result = orchestrator.executeWorkflow(plan);

            System.out.println("\n" + "=".repeat(76));
            System.out.println("EXECUTION COMPLETE");
            System.out.println("=".repeat(76));
            System.out.printf("Duration     : %.2f seconds\n", result.getDurationSeconds());
            System.out.println("Artifact Dir : " + new java.io.File(result.getOutputFilePath()).getParent());
            System.out.println("Report File  : " + result.getOutputFilePath());
            System.out.println("Generated Section Files:");
            for (var entry : result.getAgentFilePaths().entrySet()) {
                System.out.println("  - " + entry.getValue());
            }
            System.out.println("=".repeat(76) + "\n");

        } catch (Exception e) {
            System.err.println("\n[ERROR] Multi-agent execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAgentsDirectory() {
        agentRegistry.printAgentDirectory(llmClient.getModelName());
        if (lastPlan != null) {
            System.out.println("=".repeat(78));
            System.out.println("LAST DEPLOYED MULTI-AGENT DAG PLAN");
            System.out.println("=".repeat(78));
            System.out.println("Prompt : " + lastPlan.getTopic());
            System.out.println("Agents : " + lastPlan.getAgents().size());
            for (AgentDeployment a : lastPlan.getAgents()) {
                String deps = a.getDependencies().isEmpty() ? "None (Independent)" : String.join(", ", a.getDependencies());
                System.out.printf("  - [%s] %s (%s) | Priority: %s | Depends: %s\n",
                        a.getId(), a.getName(), a.getRole(), a.getPriority().getDisplayName(), deps);
                System.out.printf("    Task: %s\n", a.getResponsibility());
            }
            System.out.println("=".repeat(78) + "\n");
        }
    }

    private void showHistory() {
        System.out.println("\n" + "=".repeat(76));
        System.out.println("RECENT WORKFLOW RUNS (Persisted in Database)");
        System.out.println("=".repeat(76));

        if (runDao != null) {
            List<String> runs = runDao.getRecentRunSummaries(10);
            if (runs.isEmpty()) {
                System.out.println("  No previous workflow runs found in database.");
            } else {
                for (String r : runs) {
                    System.out.println("  " + r);
                }
            }
        } else {
            System.out.println("  RunDao persistence not available.");
        }
        System.out.println("=".repeat(76) + "\n");
    }

    private void printHelp() {
        System.out.println("\n" + "=".repeat(76));
        System.out.println("PHAROAH COMMAND REFERENCE");
        System.out.println("=".repeat(76));
        System.out.println("  <any prompt>   Directly enter your research or writing goal to deploy agents.");
        System.out.println("                 Example: write a literature review on AI in cybersecurity, then a critique of it");
        System.out.println("  /agents        Display the full open-source agent swarm directory and active DAG plan.");
        System.out.println("  /history       List recent runs retrieved from the database.");
        System.out.println("  /db            Print active database engine, connection info, row counts, & Workbench SQL.");
        System.out.println("  /model         Display active LLM model and provider.");
        System.out.println("  /help          Show this command reference.");
        System.out.println("  /exit          Terminate PHAROAH.");
        System.out.println("=".repeat(76) + "\n");
    }

    private void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     PHAROAH MULTI-AGENT SYSTEM                           ║");
        System.out.println("║            Java 21 Concurrency + Local Open-Source LLMs                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println("Type your research or writing request directly, or /help for commands.");
        System.out.println();
    }
}
