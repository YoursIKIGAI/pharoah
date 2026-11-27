package com.pharoah.orchestrator;

import com.pharoah.concurrency.ThreadPoolManager;
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
// Supports both Parallel and Priority-based scheduling using a dependency-aware ready queue.
// Saves individual and consolidated outputs to the outputs/ directory.
public class WorkflowOrchestrator {

    private final UserActivityDAO userActivityDAO;
    private final long agentLatencyMs;
    private final long executionTimeoutMs;

    public WorkflowOrchestrator(UserActivityDAO userActivityDAO, long agentLatencyMs, long executionTimeoutMs) {
        this.userActivityDAO = userActivityDAO;
        this.agentLatencyMs = Math.max(0, agentLatencyMs);
        this.executionTimeoutMs = executionTimeoutMs > 0 ? executionTimeoutMs : 120_000L;
    }

    public WorkflowOrchestrator(UserActivityDAO userActivityDAO) {
        this(userActivityDAO, 600L, 120_000L);
    }

    public WorkflowOrchestrator() {
        this(null, 600L, 120_000L);
    }

    public WorkflowExecutionResult executeWorkflow(WorkflowPlan plan) {
        int poolSize = Math.max(1, Math.min(plan.getAgents().size(), plan.getMaxConcurrentWorkers()));
        // Reusing ThreadPoolManager's shared worker thread factory instead of duplicating it
        ThreadFactory factory = ThreadPoolManager.createWorkerThreadFactory("PHAROAH-WorkerPool");

        ExecutorService executor = Executors.newFixedThreadPool(poolSize, factory);
        WorkflowContext context = new WorkflowContext(plan.getCategory().getDisplayName(), plan.getTopic(), plan.getAgents());

        Set<String> dispatchedAgentIds = Collections.synchronizedSet(new HashSet<>());
        Map<String, Future<AgentOutput>> runningFutures = new ConcurrentHashMap<>();
        AtomicBoolean isRunning = new AtomicBoolean(true);

        System.out.println("\nDeploying agents across " + poolSize + " worker threads (" + plan.getExecutionMode().getDisplayName() + ")...\n");
        System.out.println("Live Execution Status:");

        try {
            if (plan.getExecutionMode() == ExecutionMode.PRIORITY_BASED) {
                // Priority-Based Execution: Ready-queue scheduler respecting dependencies & prioritized dispatch
                executePriorityPhased(plan, context, executor, dispatchedAgentIds, runningFutures, isRunning);
            } else {
                // Parallel Execution: All eligible independent agents concurrently
                executeParallel(plan, context, executor, dispatchedAgentIds, runningFutures, isRunning);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[EXECUTION INTERRUPTED] Workflow execution thread was interrupted.");
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

    // Parallel Execution Strategy with timeout protection and failure propagation
    private void executeParallel(WorkflowPlan plan,
                                 WorkflowContext context,
                                 ExecutorService executor,
                                 Set<String> dispatchedAgentIds,
                                 Map<String, Future<AgentOutput>> runningFutures,
                                 AtomicBoolean isRunning) throws InterruptedException {

        Set<String> reportedCompleted = new HashSet<>();
        long startMs = System.currentTimeMillis();

        while (!context.isAllCompleted() && isRunning.get()) {
            // Check overall timeout
            if (System.currentTimeMillis() - startMs > executionTimeoutMs) {
                System.err.println("[TIMEOUT] Workflow execution exceeded overall timeout of " + (executionTimeoutMs / 1000) + "s. Cancelling running tasks...");
                for (Future<AgentOutput> f : runningFutures.values()) {
                    if (!f.isDone()) f.cancel(true);
                }
                break;
            }

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

            // Check completed futures and log status
            for (Map.Entry<String, Future<AgentOutput>> entry : runningFutures.entrySet()) {
                if (entry.getValue().isDone() && reportedCompleted.add(entry.getKey())) {
                    AgentDeployment agent = findAgent(plan, entry.getKey());
                    if (agent != null) {
                        String kindLabel = (agent.getKind() != null) ? " (" + agent.getKind().getDisplayName() + ")" : "";
                        try {
                            AgentOutput out = entry.getValue().get();
                            if (out != null && out.isSuccess()) {
                                System.out.println("  " + agent.getName() + kindLabel + ": Completed");
                            } else {
                                System.err.println("  " + agent.getName() + kindLabel + ": Failed (" + (out != null ? out.getContent() : "Unknown failure") + ")");
                            }
                        } catch (ExecutionException ee) {
                            System.err.println("  [ERROR] Execution exception in agent " + agent.getName() + ": " + ee.getCause());
                        } catch (CancellationException ce) {
                            System.err.println("  [CANCELLED] Agent " + agent.getName() + " was cancelled.");
                        }
                    }
                }
            }

            context.awaitStateChange(150);
        }
    }

    // VARIJ: Ready-queue priority scheduler. Dispatches only dependency-satisfied agents ordered by priority
    private void executePriorityPhased(WorkflowPlan plan,
                                       WorkflowContext context,
                                       ExecutorService executor,
                                       Set<String> dispatchedAgentIds,
                                       Map<String, Future<AgentOutput>> runningFutures,
                                       AtomicBoolean isRunning) throws InterruptedException {

        Set<String> reportedCompleted = new HashSet<>();
        long startMs = System.currentTimeMillis();

        while (!context.isAllCompleted() && isRunning.get()) {
            // Check overall timeout
            if (System.currentTimeMillis() - startMs > executionTimeoutMs) {
                System.err.println("[TIMEOUT] Priority workflow exceeded overall timeout of " + (executionTimeoutMs / 1000) + "s. Cancelling running tasks...");
                for (Future<AgentOutput> f : runningFutures.values()) {
                    if (!f.isDone()) f.cancel(true);
                }
                break;
            }

            // getEligibleReadyAgents() returns only dependency-satisfied agents, pre-sorted by Priority (HIGH -> MEDIUM -> LOW)
            List<AgentDeployment> readyAgents = context.getEligibleReadyAgents();
            for (AgentDeployment agent : readyAgents) {
                if (dispatchedAgentIds.add(agent.getId())) {
                    context.markAgentRunning(agent.getId(), "Queued");
                    String kindLabel = (agent.getKind() != null) ? " (" + agent.getKind().getDisplayName() + ")" : "";
                    // Fixed logging: using agent.getPriority().getDisplayName() to avoid "High Priority Priority"
                    System.out.println("  " + agent.getName() + kindLabel + ": Running (" + agent.getPriority().getDisplayName() + ")");

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

            // Monitor completed futures and log outputs/failures
            for (Map.Entry<String, Future<AgentOutput>> entry : runningFutures.entrySet()) {
                if (entry.getValue().isDone() && reportedCompleted.add(entry.getKey())) {
                    AgentDeployment agent = findAgent(plan, entry.getKey());
                    if (agent != null) {
                        String kindLabel = (agent.getKind() != null) ? " (" + agent.getKind().getDisplayName() + ")" : "";
                        try {
                            AgentOutput out = entry.getValue().get();
                            if (out != null && out.isSuccess()) {
                                System.out.println("  " + agent.getName() + kindLabel + ": Completed");
                            } else {
                                System.err.println("  " + agent.getName() + kindLabel + ": Failed (" + (out != null ? out.getContent() : "Unknown failure") + ")");
                            }
                        } catch (ExecutionException ee) {
                            System.err.println("  [ERROR] Execution exception in agent " + agent.getName() + ": " + ee.getCause());
                        } catch (CancellationException ce) {
                            System.err.println("  [CANCELLED] Agent " + agent.getName() + " was cancelled.");
                        }
                    }
                }
            }

            context.awaitStateChange(150);
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
        if (!outDir.exists() && !outDir.mkdirs()) {
            System.err.println("[WARNING] Could not create outputs directory.");
        }

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
                    : "Content generation was interrupted or skipped for this agent.";

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
            } catch (IOException e) {
                System.err.println("[WARNING] Failed to write agent output file (" + agentFile.getPath() + "): " + e.getMessage());
            }

            // 2. Append to Consolidated Document
            consolidatedDoc.append(a.getName()).append(": ").append(a.getResponsibility()).append("\n\n");
            consolidatedDoc.append(content).append("\n\n");
        }

        // 3. Save Consolidated Output File
        String fullDocument = consolidatedDoc.toString().trim();
        File consolidatedFile = new File(outDir, "pharoah_consolidated_" + timestamp + ".txt");
        try (FileWriter writer = new FileWriter(consolidatedFile, StandardCharsets.UTF_8)) {
            writer.write(fullDocument);
        } catch (IOException e) {
            System.err.println("[WARNING] Failed to write consolidated document (" + consolidatedFile.getPath() + "): " + e.getMessage());
        }

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
            } catch (Exception e) {
                System.err.println("[WARNING] Failed to record user activity in database: " + e.getMessage());
            }
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
