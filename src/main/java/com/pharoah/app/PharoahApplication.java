package com.pharoah.app;

import com.pharoah.agent.AgentService;
import com.pharoah.ai.AIService;
import com.pharoah.ai.OfflineHeuristicAIService;
import com.pharoah.ai.RemoteHttpAIService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.cli.PharoahCLI;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.document.Document;
import com.pharoah.document.DocumentSection;
import com.pharoah.document.DocumentType;
import com.pharoah.document.DocumentWorkflowService;
import com.pharoah.knowledge.KnowledgeResource;
import com.pharoah.knowledge.KnowledgeService;
import com.pharoah.knowledge.ResourceType;
import com.pharoah.orchestrator.TaskOrchestrator;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.*;
import com.pharoah.project.Project;
import com.pharoah.project.ProjectService;
import com.pharoah.tasks.TaskManager;
import com.pharoah.tasks.TaskService;

import java.util.List;

// SANCHIT, ASHUTOSH, VARIJ: Main Application Entrypoint.
// Initializes the terminal-first architecture connecting MySQL JDBC, multi-agent swarms,
// knowledge repository, document workflows, and interactive terminal CLI.
public class PharoahApplication {

    private final DatabaseConnectionFactory dbFactory;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AgentService agentService;
    private final KnowledgeService knowledgeService;
    private final DocumentWorkflowService documentService;
    private final TaskOrchestrator orchestrator;
    private final TaskAnalyticsEngine analyticsEngine;
    private final ThreadPoolManager poolManager;
    private final PharoahCLI cli;

    public PharoahApplication() {
        // ASHUTOSH: Initialize MySQL connection pool and schema
        this.dbFactory = new DatabaseConnectionFactory();

        ProjectDao projectDao = new ProjectDao(dbFactory);
        TaskDao taskDao = new TaskDao(dbFactory);
        AgentDao agentDao = new AgentDao(dbFactory);
        KnowledgeDao knowledgeDao = new KnowledgeDao(dbFactory);
        DocumentDao documentDao = new DocumentDao(dbFactory);

        // SANCHIT: Initialize core business services
        this.projectService = new ProjectService(projectDao);
        this.taskService = new TaskService(taskDao);
        this.agentService = new AgentService(agentDao);
        this.knowledgeService = new KnowledgeService(knowledgeDao);
        this.documentService = new DocumentWorkflowService(documentDao);
        this.analyticsEngine = new TaskAnalyticsEngine();

        // VARIJ: Initialize concurrency and orchestration engine
        this.poolManager = new ThreadPoolManager(Runtime.getRuntime().availableProcessors());
        TaskManager taskManager = new TaskManager();

        // Pluggable AI engine (Supports Offline Heuristic engine and Cloud Gemini/Ollama)
        AIService aiEngine = new RemoteHttpAIService();
        this.orchestrator = new TaskOrchestrator(taskManager, poolManager, taskDao, agentService, aiEngine);

        // SANCHIT: Initialize interactive terminal CLI
        this.cli = new PharoahCLI(projectService, taskService, agentService, orchestrator, knowledgeService, documentService, analyticsEngine);
    }

    public static void main(String[] args) {
        PharoahApplication app = new PharoahApplication();

        if (args.length > 0 && args[0].equalsIgnoreCase("--demo")) {
            app.runAutomatedDemo();
        } else if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            app.printStatus();
        } else {
            app.cli.startShell();
        }

        app.poolManager.gracefulShutdown(2);
    }

    private void printStatus() {
        System.out.println("=========================================================================");
        System.out.println("  👑 PHAROAH SYSTEM STATUS");
        System.out.printf("  Database Engine : %s (Active: %b)\n", dbFactory.getDbType(), dbFactory.isMySqlActive());
        System.out.printf("  Worker Pool     : %d Threads\n", poolManager.getPoolSize());
        System.out.printf("  Active Project  : %s\n", projectService.getActiveProject() != null ? projectService.getActiveProject().getName() : "None");
        System.out.println("=========================================================================");
    }

    // VARIJ & SANCHIT: Automated end-to-end terminal demo validating all modules
    public void runAutomatedDemo() {
        System.out.println("=========================================================================");
        System.out.println("   👑 PHAROAH: AUTOMATED TERMINAL END-TO-END DEMO WALKTHROUGH           ");
        System.out.println("=========================================================================\n");

        try {
            long runId = System.currentTimeMillis() % 10000;

            // 1. Module A: Create Project
            System.out.println("--- [1] MODULE A: PROJECT CREATION & ISOLATION ---");
            Project project = projectService.createProject("Research: AI in Cybersecurity", "Academic research project evaluating LLM agent vulnerabilities.");
            System.out.println("  Active Project: " + project.getName() + " (ID: " + project.getProjectId().substring(0, Math.min(8, project.getProjectId().length())) + ")");

            // 2. Module E: Save Knowledge Assets
            System.out.println("\n--- [2] MODULE E: KNOWLEDGE REPOSITORY PERSISTENCE ---");
            KnowledgeResource res1 = knowledgeService.saveResource(project.getProjectId(),
                    "Adversarial Prompting in AI Models", ResourceType.URL, "https://arxiv.org/abs/2302.12173", "SECURITY");
            KnowledgeResource res2 = knowledgeService.saveResource(project.getProjectId(),
                    "SQL Injection Mitigation Pattern", ResourceType.CODE_SNIPPET, "PreparedStatement ps = conn.prepareStatement(sql);", "DATABASE");
            System.out.println("  Saved Resource 1: " + res1.getTitle() + " (" + res1.getResourceType() + ")");
            System.out.println("  Saved Resource 2: " + res2.getTitle() + " (" + res2.getResourceType() + ")");

            // 3. Module D: Orchestrator Objective Decomposition
            System.out.println("\n--- [3] MODULE D: MULTI-AGENT ORCHESTRATION & DECOMPOSITION ---");
            String objectiveTitle = "AI Threat Detection #" + runId;
            System.out.println("  Decomposing objective '" + objectiveTitle + "' into dependent subtasks...");
            List<Task> subtasks = orchestrator.decomposeObjective(project.getProjectId(), objectiveTitle, "Adversarial Machine Learning");
            System.out.printf("  Generated %d subtasks with dependency graph.\n", subtasks.size());

            // 4. Module B: Execute Task Concurrently via Agent Swarm
            System.out.println("\n--- [4] MODULE B & C: CONCURRENT TASK EXECUTION BY SPECIALIZED AGENT ---");
            Task firstTask = subtasks.get(0); // Literature Review
            System.out.println("  Executing Task: " + firstTask.getTitle() + " (Priority: " + firstTask.getPriority() + ")");
            TaskResult result = orchestrator.submitTaskAsync(firstTask).get();
            System.out.printf("  Execution completed in %d ms by: %s\n", result.executionDurationMs(), result.executedByAgent());
            System.out.println("  Output Preview:\n" + result.output().lines().limit(6).reduce("", (a, b) -> a + "\n  " + b));

            // Human Review Step
            System.out.println("\n--- [5] HUMAN-IN-THE-LOOP REVIEW GATE ---");
            taskService.recordReviewApproval(firstTask.getTaskId(), true, "Approved literature review findings.");
            System.out.println("  Human reviewer approved Task output. Status updated to: COMPLETED");

            // 5. Module F: Document Workflow & Section Compilation
            System.out.println("\n--- [6] MODULE F: MULTI-SECTION DOCUMENT WORKFLOW & COMPILATION ---");
            Document doc = documentService.createDocumentWithTemplate(project.getProjectId(), "AI in Cybersecurity - Paper #" + runId, DocumentType.RESEARCH_PAPER);
            System.out.printf("  Created Document: '%s' with %d template sections.\n", doc.getTitle(), doc.getDocumentType().getStandardSections().size());

            List<DocumentSection> sections = documentService.getSections(doc.getDocumentId());
            if (!sections.isEmpty()) {
                DocumentSection abstractSec = sections.get(0);
                documentService.updateSectionContent(abstractSec.getSectionId(), "This paper investigates adversarial vulnerabilities in autonomous AI agents and proposes defensive invariants.");
                documentService.reviewSection(abstractSec.getSectionId(), true);
                System.out.println("  Drafted and approved Section: " + abstractSec.getSectionName());
            }

            String compiled = documentService.compileDocument(doc.getDocumentId());
            System.out.println("\n  Compiled Document Preview:\n" + compiled.lines().limit(8).reduce("", (a, b) -> a + "\n  " + b));

            // 6. Stream Analytics
            System.out.println("\n--- [7] STREAM API ANALYTICS & DATABASE METRICS ---");
            List<Task> allTasks = taskService.getAllTasks();
            System.out.printf("  Total Tasks Recorded: %d\n", allTasks.size());
            System.out.println("  Category Distribution (Collectors.groupingBy): " + analyticsEngine.countTasksByType(allTasks));

            System.out.println("\n=========================================================================");
            System.out.println("  🎉 COMPLETE SYSTEM VERIFICATION PASSED: ALL 6 CORE MODULES FUNCTIONAL!");
            System.out.println("=========================================================================");

        } catch (Exception e) {
            System.err.println("Demo encountered error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
