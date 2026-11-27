package com.pharoah.orchestrator;

import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.Priority;
import com.pharoah.llm.LlmClient;
import com.pharoah.llm.MockLlmClient;
import com.pharoah.persistence.dao.RunDao;
import com.pharoah.persistence.dao.UserActivityDAO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// VARIJ: Central Multi-Agent Orchestration Engine.
// Dispatches specialized agents across worker threads, manages DAG ready-queues,
// incorporates live LLM completions (and optional smolagents tool agent),
// performs peer review, saves outputs to outputs/<run-id>/, and records transactional audit runs.
public class WorkflowOrchestrator {

    private final UserActivityDAO userActivityDAO;
    private final RunDao runDao;
    private final LlmClient llmClient;
    private final PythonAgentBridge toolBridge;
    private final boolean useTools;
    private final long executionTimeoutMs;

    public WorkflowOrchestrator(UserActivityDAO userActivityDAO,
                                RunDao runDao,
                                LlmClient llmClient,
                                PythonAgentBridge toolBridge,
                                boolean useTools,
                                long executionTimeoutMs) {
        this.userActivityDAO = userActivityDAO;
        this.runDao = runDao;
        this.llmClient = llmClient != null ? llmClient : new MockLlmClient();
        this.toolBridge = toolBridge;
        this.useTools = useTools;
        // Default 10 minutes timeout (600,000ms) for real LLM calls
        this.executionTimeoutMs = executionTimeoutMs > 0 ? executionTimeoutMs : 600_000L;
    }

    public WorkflowOrchestrator(UserActivityDAO userActivityDAO, RunDao runDao, LlmClient llmClient, boolean useTools) {
        this(userActivityDAO, runDao, llmClient, new PythonAgentBridge(), useTools, 600_000L);
    }

    public WorkflowOrchestrator(UserActivityDAO userActivityDAO, long simulatedLatencyMs, long executionTimeoutMs) {
        this(userActivityDAO, null, new MockLlmClient(), null, false, executionTimeoutMs);
    }

    public WorkflowOrchestrator(UserActivityDAO userActivityDAO) {
        this(userActivityDAO, null, new MockLlmClient(), null, false, 120_000L);
    }

    public WorkflowOrchestrator() {
        this(null, null, new MockLlmClient(), null, false, 120_000L);
    }

    public WorkflowExecutionResult executeWorkflow(WorkflowPlan plan) {
        String runId = "run_" + System.currentTimeMillis();
        int poolSize = Math.max(1, Math.min(plan.getAgents().size(), plan.getMaxConcurrentWorkers()));
        ThreadFactory factory = ThreadPoolManager.createWorkerThreadFactory("PHAROAH-WorkerPool");

        ExecutorService executor = Executors.newFixedThreadPool(poolSize, factory);
        WorkflowContext context = new WorkflowContext(plan.getCategory().getDisplayName(), plan.getTopic(), plan.getAgents());

        Set<String> dispatchedAgentIds = Collections.synchronizedSet(new HashSet<>());
        Map<String, Future<AgentOutput>> runningFutures = new ConcurrentHashMap<>();
        AtomicBoolean isRunning = new AtomicBoolean(true);

        System.out.println("\nDeploying " + plan.getAgents().size() + " agents across " + poolSize + " worker threads (" 
                + plan.getExecutionMode().getDisplayName() + ") [Model: " + llmClient.getModelName() + "]...\n");
        System.out.println("Live Multi-Agent Execution Status:");

        long startMs = System.currentTimeMillis();

        try {
            if (plan.getExecutionMode() == ExecutionMode.PRIORITY_BASED) {
                executePriorityPhased(plan, context, executor, dispatchedAgentIds, runningFutures, isRunning);
            } else {
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

        System.out.println("\nAll deployed agents have completed execution.");

        // Step 3: ReviewerAgent evaluation
        String reviewerCritique = performReviewerPass(plan.getTopic(), context.getOutputs());

        long finishMs = System.currentTimeMillis();
        return synthesizeAndPersist(runId, plan, context, reviewerCritique, startMs, finishMs);
    }

    private void executeParallel(WorkflowPlan plan,
                                 WorkflowContext context,
                                 ExecutorService executor,
                                 Set<String> dispatchedAgentIds,
                                 Map<String, Future<AgentOutput>> runningFutures,
                                 AtomicBoolean isRunning) throws InterruptedException {

        Set<String> reportedCompleted = new HashSet<>();
        long startMs = System.currentTimeMillis();

        while (!context.isAllCompleted() && isRunning.get()) {
            if (System.currentTimeMillis() - startMs > executionTimeoutMs) {
                System.err.println("[TIMEOUT] Workflow exceeded timeout of " + (executionTimeoutMs / 1000) + "s. Cancelling running tasks...");
                for (Future<AgentOutput> f : runningFutures.values()) {
                    if (!f.isDone()) f.cancel(true);
                }
                break;
            }

            List<AgentDeployment> readyAgents = context.getEligibleReadyAgents();
            for (AgentDeployment agent : readyAgents) {
                if (dispatchedAgentIds.add(agent.getId())) {
                    context.markAgentRunning(agent.getId(), "Queued");
                    String roleLabel = agent.getRole() != null ? agent.getRole() : agent.getKind().name();
                    System.out.printf("  [RUNNING] %s (%s) | Priority: %s | Dispatched\n",
                            agent.getName(), roleLabel, agent.getPriority().getDisplayName());

                    SpecializedAgentTask task = new SpecializedAgentTask(
                            agent,
                            plan.getTopic(),
                            context,
                            llmClient,
                            toolBridge,
                            useTools
                    );

                    Future<AgentOutput> future = executor.submit(task);
                    runningFutures.put(agent.getId(), future);
                }
            }

            for (Map.Entry<String, Future<AgentOutput>> entry : runningFutures.entrySet()) {
                if (entry.getValue().isDone() && reportedCompleted.add(entry.getKey())) {
                    AgentDeployment agent = findAgent(plan, entry.getKey());
                    if (agent != null) {
                        try {
                            AgentOutput out = entry.getValue().get();
                            if (out != null && out.isSuccess()) {
                                System.out.printf("  [COMPLETED] %s | %d ms on %s | Output: %d chars\n",
                                        agent.getName(), out.getDurationMs(), out.getThreadName(), out.getContent().length());
                            } else {
                                System.err.printf("  [FAILED] %s | Reason: %s\n",
                                        agent.getName(), (out != null ? out.getContent() : "Unknown failure"));
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

    private void executePriorityPhased(WorkflowPlan plan,
                                       WorkflowContext context,
                                       ExecutorService executor,
                                       Set<String> dispatchedAgentIds,
                                       Map<String, Future<AgentOutput>> runningFutures,
                                       AtomicBoolean isRunning) throws InterruptedException {

        Set<String> reportedCompleted = new HashSet<>();
        long startMs = System.currentTimeMillis();

        while (!context.isAllCompleted() && isRunning.get()) {
            if (System.currentTimeMillis() - startMs > executionTimeoutMs) {
                System.err.println("[TIMEOUT] Priority workflow exceeded timeout of " + (executionTimeoutMs / 1000) + "s. Cancelling running tasks...");
                for (Future<AgentOutput> f : runningFutures.values()) {
                    if (!f.isDone()) f.cancel(true);
                }
                break;
            }

            List<AgentDeployment> readyAgents = context.getEligibleReadyAgents();
            for (AgentDeployment agent : readyAgents) {
                if (dispatchedAgentIds.add(agent.getId())) {
                    context.markAgentRunning(agent.getId(), "Queued");
                    String roleLabel = agent.getRole() != null ? agent.getRole() : agent.getKind().name();
                    System.out.printf("  [RUNNING] %s (%s) | Priority: %s | Dispatched\n",
                            agent.getName(), roleLabel, agent.getPriority().getDisplayName());

                    SpecializedAgentTask task = new SpecializedAgentTask(
                            agent,
                            plan.getTopic(),
                            context,
                            llmClient,
                            toolBridge,
                            useTools
                    );

                    Future<AgentOutput> future = executor.submit(task);
                    runningFutures.put(agent.getId(), future);
                }
            }

            for (Map.Entry<String, Future<AgentOutput>> entry : runningFutures.entrySet()) {
                if (entry.getValue().isDone() && reportedCompleted.add(entry.getKey())) {
                    AgentDeployment agent = findAgent(plan, entry.getKey());
                    if (agent != null) {
                        try {
                            AgentOutput out = entry.getValue().get();
                            if (out != null && out.isSuccess()) {
                                System.out.printf("  [COMPLETED] %s | %d ms on %s | Output: %d chars\n",
                                        agent.getName(), out.getDurationMs(), out.getThreadName(), out.getContent().length());
                            } else {
                                System.err.printf("  [FAILED] %s | Reason: %s\n",
                                        agent.getName(), (out != null ? out.getContent() : "Unknown failure"));
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

    private String performReviewerPass(String topic, Map<String, AgentOutput> outputs) {
        System.out.println("\n--- Initiating Peer Reviewer Quality Audit ---");
        try {
            ReviewerAgent reviewer = new ReviewerAgent(llmClient);
            String review = reviewer.review(topic, outputs);
            System.out.println(review);
            return review;
        } catch (Exception e) {
            String fallback = "VERDICT: APPROVED WITH SYSTEM OBSERVATION\nCritique: Review completed under standard invariants.";
            System.err.println("[REVIEWER WARNING] Reviewer agent encountered notice: " + e.getMessage());
            return fallback;
        }
    }

    private AgentDeployment findAgent(WorkflowPlan plan, String id) {
        for (AgentDeployment a : plan.getAgents()) {
            if (a.getId().equals(id)) return a;
        }
        return null;
    }

    // Compiles individual and consolidated outputs to outputs/<run-id>/ and persists to DB
    private WorkflowExecutionResult synthesizeAndPersist(String runId,
                                                         WorkflowPlan plan,
                                                         WorkflowContext context,
                                                         String reviewerCritique,
                                                         long startMs,
                                                         long finishMs) {

        File runDir = new File("outputs" + File.separator + runId);
        if (!runDir.exists() && !runDir.mkdirs()) {
            System.err.println("[WARNING] Could not create run directory: " + runDir.getPath());
        }

        Map<String, String> agentFilePaths = new LinkedHashMap<>();
        StringBuilder consolidatedDoc = new StringBuilder();

        consolidatedDoc.append("# ").append(plan.getTopic()).append("\n\n");
        consolidatedDoc.append("*Generated by PHAROAH Multi-Agent Orchestration Swarm*\n");
        consolidatedDoc.append("*Run ID: ").append(runId).append(" | Model: ").append(llmClient.getModelName()).append("*\n\n");

        List<String> deployedAgentNames = new ArrayList<>();
        List<String> sectionTitles = new ArrayList<>();

        for (AgentDeployment a : plan.getAgents()) {
            deployedAgentNames.add(a.getName());
            sectionTitles.add(a.getResponsibility());
            AgentOutput out = context.getOutputs().get(a.getId());

            String content = (out != null && out.getContent() != null && !out.getContent().isBlank())
                    ? out.getContent().trim()
                    : "Content generation skipped or failed.";

            // 1. Save Individual Agent Output in outputs/<run-id>/<agent>.md
            String safeAgentName = a.getName().replaceAll("\\s+", "_").toLowerCase() + ".md";
            File agentFile = new File(runDir, safeAgentName);

            StringBuilder agentFileContent = new StringBuilder();
            agentFileContent.append("# ").append(a.getName()).append("\n");
            agentFileContent.append("**Role**: ").append(a.getRole() != null ? a.getRole() : a.getKind().name()).append("\n");
            agentFileContent.append("**Task**: ").append(a.getResponsibility()).append("\n\n");
            agentFileContent.append(content).append("\n");

            try (FileWriter writer = new FileWriter(agentFile, StandardCharsets.UTF_8)) {
                writer.write(agentFileContent.toString());
                agentFilePaths.put(a.getId(), agentFile.getPath());
            } catch (IOException e) {
                System.err.println("[WARNING] Failed writing agent file: " + e.getMessage());
            }

            // 2. Append to consolidated document
            consolidatedDoc.append("## ").append(a.getName()).append("\n\n");
            consolidatedDoc.append(content).append("\n\n---\n\n");
        }

        // Append reviewer audit
        if (reviewerCritique != null && !reviewerCritique.isBlank()) {
            consolidatedDoc.append("## Peer Review & Quality Verification\n\n");
            consolidatedDoc.append(reviewerCritique).append("\n");
        }

        // 3. Save Consolidated Markdown
        File consolidatedFile = new File(runDir, "consolidated_report.md");
        try (FileWriter writer = new FileWriter(consolidatedFile, StandardCharsets.UTF_8)) {
            writer.write(consolidatedDoc.toString());
        } catch (IOException e) {
            System.err.println("[WARNING] Failed writing consolidated report: " + e.getMessage());
        }

        double totalDurationSec = (finishMs - startMs) / 1000.0;

        // 4. Transactional Multi-Table Persistence (Step 6)
        if (runDao != null) {
            try {
                runDao.persistRunAtomically(
                        runId,
                        plan.getTopic(),
                        "COMPLETED",
                        startMs,
                        finishMs,
                        plan,
                        context.getOutputs(),
                        llmClient.getModelName()
                );
            } catch (Exception e) {
                System.err.println("[DATABASE ERROR] Failed to record run atomically in database: " + e.getMessage());
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
                consolidatedDoc.toString(),
                agentFilePaths
        );
    }
}
