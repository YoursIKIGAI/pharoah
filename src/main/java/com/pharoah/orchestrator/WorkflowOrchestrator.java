package com.pharoah.orchestrator;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.model.UserActivity;
import com.pharoah.persistence.dao.UserActivityDAO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// VARIJ: Central Multi-Agent Orchestration Engine.
// Executes agents concurrently using Java ExecutorService & FixedThreadPool.
// Supports both Parallel and Priority-based scheduling.
// Saves individual and consolidated outputs to the outputs/ directory.
public class WorkflowOrchestrator {

    private final UserActivityDAO userActivityDAO;
    private final long agentLatencyMs;

    public WorkflowOrchestrator(UserActivityDAO userActivityDAO) {
        this.userActivityDAO = userActivityDAO;
        this.agentLatencyMs = 600L;
    }

    public WorkflowOrchestrator() {
        this(null);
    }

    public WorkflowExecutionResult executeWorkflow(WorkflowPlan plan) {
        int poolSize = Math.max(1, Math.min(plan.getAgents().size(), plan.getMaxConcurrentWorkers()));
        ThreadFactory factory = new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "PHAROAH-WorkerPool-" + count++);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(poolSize, factory);
        WorkflowContext context = new WorkflowContext(plan.getCategory().getDisplayName(), plan.getTopic(), plan.getAgents());

        Set<String> dispatchedAgentIds = Collections.synchronizedSet(new HashSet<>());
        Map<String, Future<AgentOutput>> runningFutures = new ConcurrentHashMap<>();
        AtomicBoolean isRunning = new AtomicBoolean(true);

        System.out.println("\nDeploying agents across " + poolSize + " worker threads (" + plan.getExecutionMode().getDisplayName() + ")...\n");
        System.out.println("Live Execution Status:");

        try {
            if (plan.getExecutionMode() == ExecutionMode.PRIORITY_BASED) {
                // Priority-Based Execution: HIGH -> MEDIUM -> LOW
                executePriorityPhased(plan, context, executor, dispatchedAgentIds, runningFutures);
            } else {
                // Parallel Execution: All eligible independent agents concurrently
                executeParallel(plan, context, executor, dispatchedAgentIds, runningFutures, isRunning);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Execution was interrupted.");
        } finally {
            isRunning.set(false);
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                executor.shutdownNow();
            }
        }

        System.out.println("\nAll agents have completed their execution.");
        return synthesizeAndPersist(plan, context);
    }

    // Parallel Execution Strategy
    private void executeParallel(WorkflowPlan plan,
                                 WorkflowContext context,
                                 ExecutorService executor,
                                 Set<String> dispatchedAgentIds,
                                 Map<String, Future<AgentOutput>> runningFutures,
                                 AtomicBoolean isRunning) throws InterruptedException {

        Set<String> reportedCompleted = new HashSet<>();

        while (!context.isAllCompleted() && isRunning.get()) {
            List<AgentDeployment> readyAgents = context.getEligibleReadyAgents();
            for (AgentDeployment agent : readyAgents) {
                if (dispatchedAgentIds.add(agent.getId())) {
                    context.markAgentRunning(agent.getId(), "Queued");
                    String kindLabel = (agent.getKind() != null) ? " (" + agent.getKind().getDisplayName() + ")" : "";
                    System.out.println("  " + agent.getName() + kindLabel + ": Running");

                    SpecializedAgentTask task = new SpecializedAgentTask(
                            agent,
                            agent.getResponsibility(),
                            context,
                            agentLatencyMs
                    );

                    Future<AgentOutput> future = executor.submit(task);
                    runningFutures.put(agent.getId(), future);
                }
            }

            // Check completed futures
            for (Map.Entry<String, Future<AgentOutput>> entry : runningFutures.entrySet()) {
                if (entry.getValue().isDone() && reportedCompleted.add(entry.getKey())) {
                    AgentDeployment agent = findAgent(plan, entry.getKey());
                    if (agent != null) {
                        String kindLabel = (agent.getKind() != null) ? " (" + agent.getKind().getDisplayName() + ")" : "";
                        System.out.println("  " + agent.getName() + kindLabel + ": Completed");
                    }
                }
            }

            context.awaitStateChange(150);
        }
    }

    // Priority-Based Execution Strategy (HIGH -> MEDIUM -> LOW)
    private void executePriorityPhased(WorkflowPlan plan,
                                       WorkflowContext context,
                                       ExecutorService executor,
                                       Set<String> dispatchedAgentIds,
                                       Map<String, Future<AgentOutput>> runningFutures) throws InterruptedException {

        Priority[] priorityPhases = new Priority[]{Priority.HIGH, Priority.MEDIUM, Priority.LOW};

        for (Priority phase : priorityPhases) {
            List<AgentDeployment> phaseAgents = new ArrayList<>();
            for (AgentDeployment a : plan.getAgents()) {
                if (a.getPriority() == phase) {
                    phaseAgents.add(a);
                }
            }

            if (phaseAgents.isEmpty()) continue;

            // Mark other lower-priority agents as waiting
            for (AgentDeployment a : plan.getAgents()) {
                if (a.getPriority() != phase && !dispatchedAgentIds.contains(a.getId())) {
                    String kindLabel = (a.getKind() != null) ? " (" + a.getKind().getDisplayName() + ")" : "";
                    System.out.println("  " + a.getName() + kindLabel + ": Waiting");
                }
            }

            // Dispatch all agents of this priority phase
            List<Future<AgentOutput>> phaseFutures = new ArrayList<>();
            for (AgentDeployment agent : phaseAgents) {
                if (dispatchedAgentIds.add(agent.getId())) {
                    context.markAgentRunning(agent.getId(), "Queued");
                    String kindLabel = (agent.getKind() != null) ? " (" + agent.getKind().getDisplayName() + ")" : "";
                    System.out.println("  " + agent.getName() + kindLabel + ": Running (" + phase + " Priority)");

                    SpecializedAgentTask task = new SpecializedAgentTask(
                            agent,
                            agent.getResponsibility(),
                            context,
                            agentLatencyMs
                    );

                    Future<AgentOutput> future = executor.submit(task);
                    phaseFutures.add(future);
                    runningFutures.put(agent.getId(), future);
                }
            }

            // Wait for this priority phase to complete before advancing to next priority
            for (int i = 0; i < phaseFutures.size(); i++) {
                try {
                    phaseFutures.get(i).get();
                    AgentDeployment completedAgent = phaseAgents.get(i);
                    String kindLabel = (completedAgent.getKind() != null) ? " (" + completedAgent.getKind().getDisplayName() + ")" : "";
                    System.out.println("  " + completedAgent.getName() + kindLabel + ": Completed");
                } catch (ExecutionException ignored) {}
            }
        }
    }

    private AgentDeployment findAgent(WorkflowPlan plan, String id) {
        for (AgentDeployment a : plan.getAgents()) {
            if (a.getId().equals(id)) return a;
        }
        return null;
    }

    // Compiles individual and consolidated outputs to the outputs/ directory
    private WorkflowExecutionResult synthesizeAndPersist(WorkflowPlan plan, WorkflowContext context) {
        File outDir = new File("outputs");
        if (!outDir.exists()) outDir.mkdirs();

        long timestamp = System.currentTimeMillis();
        Map<String, String> agentFilePaths = new LinkedHashMap<>();
        StringBuilder consolidatedDoc = new StringBuilder();

        List<String> deployedAgentNames = new ArrayList<>();
        List<String> sectionTitles = new ArrayList<>();

        for (AgentDeployment a : plan.getAgents()) {
            deployedAgentNames.add(a.getName());
            sectionTitles.add(a.getResponsibility());
            AgentOutput out = context.getOutputs().get(a.getId());

            String content = (out != null && out.getContent() != null && !out.getContent().isBlank())
                    ? out.getContent().trim()
                    : "Content generation was interrupted for this agent.";

            // 1. Prepare Individual Agent Output File
            StringBuilder agentFileContent = new StringBuilder();
            agentFileContent.append(a.getName()).append(": ").append(a.getResponsibility()).append("\n");
            if (a.getKind() != null) {
                agentFileContent.append("Specialization: ").append(a.getKind().getDisplayName()).append("\n");
            }
            agentFileContent.append("\n").append(content).append("\n");

            String safeName = a.getName().replaceAll("\\s+", "_") + "_" +
                    (a.getKind() != null ? a.getKind().name() : "Agent") + "_" + timestamp + ".txt";
            File agentFile = new File(outDir, safeName);

            try (FileWriter writer = new FileWriter(agentFile, StandardCharsets.UTF_8)) {
                writer.write(agentFileContent.toString());
                agentFilePaths.put(a.getId(), agentFile.getPath());
            } catch (IOException ignored) {}

            // 2. Append to Consolidated Document
            consolidatedDoc.append(a.getName()).append(": ").append(a.getResponsibility()).append("\n\n");
            consolidatedDoc.append(content).append("\n\n");
        }

        // 3. Save Consolidated Output File
        String fullDocument = consolidatedDoc.toString().trim();
        File consolidatedFile = new File(outDir, "pharoah_consolidated_" + timestamp + ".txt");
        try (FileWriter writer = new FileWriter(consolidatedFile, StandardCharsets.UTF_8)) {
            writer.write(fullDocument);
        } catch (IOException ignored) {}

        double totalDurationSec = context.getElapsedTimeMs() / 1000.0;

        // 4. Persist Audit in MySQL user_activity table
        String sectionsWorked = String.join(", ", sectionTitles);
        String agentsUsed = String.join(", ", deployedAgentNames);

        if (userActivityDAO != null) {
            try {
                UserActivity activity = new UserActivity(
                        "User",
                        plan.getTopic(),
                        plan.getExecutionMode().getDisplayName(),
                        sectionsWorked,
                        agentsUsed
                );
                userActivityDAO.recordActivity(activity);
            } catch (Exception ignored) {}
        }

        return new WorkflowExecutionResult(
                !context.getOutputs().isEmpty(),
                plan.getTopic(),
                plan.getExecutionMode().getDisplayName(),
                consolidatedFile.getPath(),
                totalDurationSec,
                deployedAgentNames,
                sectionTitles,
                context.getOutputs(),
                fullDocument,
                agentFilePaths
        );
    }
}
