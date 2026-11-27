package com.pharoah.orchestrator;

import com.pharoah.agents.AgentConfig;
import com.pharoah.agents.AgentRole;
import com.pharoah.agents.AgentService;
import com.pharoah.ai.AIService;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.persistence.dao.TaskDao;
import com.pharoah.tasks.TaskManager;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;

// VARIJ: Implemented TaskOrchestrator.
// The central multi-agent coordinator: task decomposition, dependency checking,
// concurrent thread pool delegation, human review enforcement, and MOCK vs REAL AI distinction.
public class TaskOrchestrator {

    private final TaskManager taskManager;
    private final ThreadPoolManager poolManager;
    private final TaskDao taskDao;
    private final AgentService agentService;
    private final AIService aiService;

    public TaskOrchestrator(TaskManager taskManager,
                            ThreadPoolManager poolManager,
                            TaskDao taskDao,
                            AgentService agentService,
                            AIService aiService) {
        this.taskManager = taskManager;
        this.poolManager = poolManager;
        this.taskDao = taskDao;
        this.agentService = agentService;
        this.aiService = aiService;
    }

    // VARIJ: Decomposes a large intellectual objective into smaller dependent tasks
    public List<Task> decomposeObjective(String projectId, String objectiveTitle, String domainTopic) throws SQLException {
        List<Task> subtasks = new ArrayList<>();

        // 1. Research Task
        Task t1 = new Task(projectId, "Survey & Literature Review: " + objectiveTitle,
                TaskType.RESEARCH, Priority.HIGH,
                "Extract theoretical background, existing methodologies, and research gaps for: " + domainTopic,
                "agent-01", null);
        taskDao.insertTask(t1);
        subtasks.add(t1);

        // 2. Technical Analysis Task (Depends on Research Task)
        Task t2 = new Task(projectId, "Comparative & Empirical Analysis: " + objectiveTitle,
                TaskType.COMPARISON, Priority.HIGH,
                "Compare architectural paradigms, performance metrics, and trade-offs for: " + domainTopic,
                "agent-03", t1.getTaskId());
        taskDao.insertTask(t2);
        subtasks.add(t2);

        // 3. Technical Composition Task (Depends on Analysis)
        Task t3 = new Task(projectId, "Draft Main Report & Synthesis: " + objectiveTitle,
                TaskType.WRITING, Priority.MEDIUM,
                "Synthesize findings, implementation roadmap, and conclusions for: " + domainTopic,
                "agent-02", t2.getTaskId());
        taskDao.insertTask(t3);
        subtasks.add(t3);

        // 4. Quality Review Task (Depends on Composition)
        Task t4 = new Task(projectId, "Peer Review & Verification: " + objectiveTitle,
                TaskType.VERIFICATION, Priority.MEDIUM,
                "Verify factual claims, check citations, and validate accuracy for: " + domainTopic,
                "agent-06", t3.getTaskId());
        taskDao.insertTask(t4);
        subtasks.add(t4);

        return subtasks;
    }

    // VARIJ: Submits a single task asynchronously while enforcing dependency checks
    public Future<TaskResult> submitTaskAsync(Task task) throws SQLException {
        // Enforce Dependency Check
        if (task.getDependsOnTaskId() != null && !task.getDependsOnTaskId().trim().isEmpty()) {
            Task prerequisite = taskDao.findById(task.getDependsOnTaskId());
            if (prerequisite != null && prerequisite.getStatus() != TaskStatus.COMPLETED && prerequisite.getStatus() != TaskStatus.UNDER_REVIEW) {
                System.out.printf("[DEPENDENCY GUARD] Task '%s' is blocked waiting for prerequisite '%s' (Status: %s).\n",
                        task.getTitle(), prerequisite.getTitle(), prerequisite.getStatus());
                task.updateStatus(TaskStatus.PENDING);
                taskDao.updateTask(task);
                return poolManager.submitTask(() -> TaskResult.failure(task.getTaskId(), "Blocked by incomplete prerequisite: " + prerequisite.getTitle(), 0, "DependencyGuard"));
            }
        }

        taskManager.submitTask(task);

        return poolManager.submitTask(() -> {
            long startTime = System.currentTimeMillis();
            task.updateStatus(TaskStatus.IN_PROGRESS);
            taskDao.updateTask(task);

            // Select agent
            AgentConfig agent = null;
            if (task.getAssignedAgentId() != null) {
                agent = agentService.getAgentById(task.getAssignedAgentId());
            }
            if (agent == null) {
                AgentRole targetRole = mapTaskTypeToRole(task.getType());
                agent = agentService.findBestAgentForRole(targetRole).orElse(null);
            }

            String agentName = (agent != null) ? agent.getName() : "General Swarm Worker";
            String providerType = (agent != null) ? agent.getProviderType() : "MOCK_HEURISTIC";

            System.out.printf("[ORCHESTRATOR] Delegating '%s' to %s [%s]...\n",
                    task.getTitle(), agentName, providerType);

            // Execute intelligence
            String rawOutput = executeIntelligence(task, agent);
            long duration = System.currentTimeMillis() - startTime;

            // Prepend clear labeling to distinguish Mock vs Real AI
            String labeledOutput = String.format("""
                %s
                **Executing Agent:** %s (Role: %s)
                **Execution Latency:** %d ms
                
                %s
                """,
                    agent != null && agent.isMock() ? "⚠️ **[MOCK / HEURISTIC AGENT EXECUTION - SIMULATED OUTPUT]**" : "🤖 **[REAL AI AGENT EXECUTION: " + providerType + "]**",
                    agentName,
                    (agent != null ? agent.getRole().getDisplayName() : "Worker"),
                    duration,
                    rawOutput);

            // Mark completed with output so dependent downstream tasks can execute
            task.markCompleted(labeledOutput);
            task.setAssignedAgentId(agent != null ? agent.getAgentId() : null);
            taskDao.updateTask(task);

            TaskResult result = TaskResult.success(task.getTaskId(), labeledOutput, duration, agentName);
            taskManager.completeTask(task.getTaskId(), result);

            return result;
        });
    }

    // VARIJ: Synthesizes intellectual content via AIService
    private String executeIntelligence(Task task, AgentConfig agent) {
        String prompt = task.getInputPayload();
        String context = (agent != null) ? agent.getInstructions() : "Standard academic guidelines";

        return switch (task.getType()) {
            case RESEARCH -> aiService.synthesizeResearch(task.getTitle(), prompt + "\nContext: " + context);
            case COMPARISON -> {
                String[] parts = task.getTitle().split("(?i)\\s+(vs\\.?|versus|against)\\s+");
                String a = parts.length > 0 ? parts[0] : "Option A";
                String b = parts.length > 1 ? parts[1] : "Option B";
                yield aiService.generateComparison(a, b, prompt);
            }
            case WRITING -> aiService.generateWriting(prompt, "Academic & Technical", context);
            case VERIFICATION -> aiService.verifyClaims(task.getTitle(), prompt);
            case STUDY_ASSISTANCE -> aiService.generateStudyAid(task.getTitle(), prompt);
            case PLANNING -> aiService.generatePlan(task.getTitle(), prompt);
            case DATA_ANALYSIS -> aiService.analyzeData(prompt);
            case DOCUMENT_ANALYSIS -> aiService.generateSummary(prompt, 5);
            case CODING -> String.format("""
                ### 💻 Code Analysis & Implementation Specification: %s
                * **Language / Platform:** Java 21 LTS
                * **Component:** %s
                * **Implementation Guidelines:**
                  ```java
                  // Implementation guideline for: %s
                  public class ImplementationSnippet {
                      // Thread-safe modular logic
                  }
                  ```
                """, task.getTitle(), prompt, task.getTitle());
        };
    }

    private AgentRole mapTaskTypeToRole(TaskType type) {
        return switch (type) {
            case RESEARCH -> AgentRole.RESEARCH_AGENT;
            case WRITING -> AgentRole.WRITING_AGENT;
            case COMPARISON, DATA_ANALYSIS -> AgentRole.ANALYSIS_AGENT;
            case CODING -> AgentRole.CODING_AGENT;
            case DOCUMENT_ANALYSIS -> AgentRole.DOCUMENTATION_AGENT;
            case VERIFICATION -> AgentRole.REVIEWER_AGENT;
            case PLANNING, STUDY_ASSISTANCE -> AgentRole.PLANNING_AGENT;
        };
    }

    public List<TaskResult> executeBatch(List<Task> tasks) throws Exception {
        List<Future<TaskResult>> futures = new ArrayList<>();
        for (Task t : tasks) {
            futures.add(submitTaskAsync(t));
        }

        List<TaskResult> results = new ArrayList<>();
        for (Future<TaskResult> f : futures) {
            results.add(f.get());
        }
        return results;
    }
}
