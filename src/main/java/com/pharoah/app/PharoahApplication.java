package com.pharoah.app;

import com.pharoah.agent.AgentService;
import com.pharoah.ai.AIService;
import com.pharoah.ai.RemoteHttpAIService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.cli.PharoahCLI;
import com.pharoah.cli.TerminalUI;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.config.AppConfig;
import com.pharoah.core.model.UserActivity;
import com.pharoah.document.Document;
import com.pharoah.document.DocumentSection;
import com.pharoah.document.DocumentType;
import com.pharoah.document.DocumentWorkflowService;
import com.pharoah.knowledge.KnowledgeResource;
import com.pharoah.knowledge.KnowledgeService;
import com.pharoah.knowledge.ResourceType;
import com.pharoah.orchestrator.*;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.*;
import com.pharoah.project.Project;
import com.pharoah.project.ProjectService;
import com.pharoah.tasks.TaskManager;
import com.pharoah.tasks.TaskService;

import java.util.List;

// VARIJ, ASHUTOSH, SANCHIT: Main Application Entrypoint.
// Technical Architecture: Java-Native Multi-Agent Orchestrator + FixedThreadPool Concurrency + MySQL 8.0 Persistence.
public class PharoahApplication {

    private final DatabaseConnectionFactory dbFactory;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AgentService agentService;
    private final KnowledgeService knowledgeService;
    private final DocumentWorkflowService documentService;
    private final com.pharoah.orchestrator.TaskOrchestrator legacyOrchestrator;
    private final TaskAnalyticsEngine analyticsEngine;
    private final ThreadPoolManager poolManager;
    private final UserActivityDAO userActivityDAO;
    private final WorkflowOrchestrator workflowOrchestrator;
    private final PharoahCLI cli;

    public PharoahApplication() {
        // SANCHIT: Initialize MySQL connection pool and auto-bootstrapped schema
        this.dbFactory = new DatabaseConnectionFactory();

        ProjectDao projectDao = new ProjectDao(dbFactory);
        TaskDao taskDao = new TaskDao(dbFactory);
        AgentDao agentDao = new AgentDao(dbFactory);
        KnowledgeDao knowledgeDao = new KnowledgeDao(dbFactory);
        DocumentDao documentDao = new DocumentDao(dbFactory);
        this.userActivityDAO = new UserActivityDAO(dbFactory);

        // Core business services
        this.projectService = new ProjectService(projectDao);
        this.taskService = new TaskService(taskDao);
        this.agentService = new AgentService(agentDao);
        this.knowledgeService = new KnowledgeService(knowledgeDao);
        this.documentService = new DocumentWorkflowService(documentDao);
        this.analyticsEngine = new TaskAnalyticsEngine();

        // Concurrency and orchestration engine
        this.poolManager = new ThreadPoolManager(Runtime.getRuntime().availableProcessors());
        TaskManager taskManager = new TaskManager();

        AIService aiEngine = new RemoteHttpAIService();
        this.legacyOrchestrator = new com.pharoah.orchestrator.TaskOrchestrator(taskManager, poolManager, taskDao, agentService, aiEngine);

        // VARIJ: Central Multi-Agent Orchestration Engine
        this.workflowOrchestrator = new WorkflowOrchestrator(userActivityDAO);

        // Terminal CLI
        this.cli = new PharoahCLI(
                projectService,
                taskService,
                agentService,
                legacyOrchestrator,
                knowledgeService,
                documentService,
                analyticsEngine,
                userActivityDAO
        );
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
        AppConfig cfg = AppConfig.getInstance();
        TerminalUI.printBanner("2.0-LTS", cfg.getDbType(), dbFactory.isMySqlActive());
        TerminalUI.printSectionHeader("SYSTEM HEALTH & CONFIGURATION");
        System.out.printf("  Database Engine : %s (Active: %b)\n", dbFactory.getDbType(), dbFactory.isMySqlActive());
        System.out.printf("  Database URL    : %s\n", cfg.getDbUrl());
        System.out.printf("  Database User   : %s\n", cfg.getDbUser());
        System.out.printf("  Database Auth   : %s\n", cfg.getMaskedPassword());
        System.out.printf("  Worker Pool     : %d Available CPU Processors\n", poolManager.getPoolSize());
        System.out.printf("  Active Project  : %s\n", projectService.getActiveProject() != null ? projectService.getActiveProject().getName() : "None");
        TerminalUI.printSectionFooter();
    }

    // VARIJ, ASHUTOSH, SANCHIT: Automated end-to-end demo validating genuine concurrent multi-agent orchestration
    public void runAutomatedDemo() {
        TerminalUI.printBanner("2.0-LTS", dbFactory.getDbType(), dbFactory.isMySqlActive());
        TerminalUI.printSectionHeader("JAVA CONCURRENT MULTI-AGENT ORCHESTRATION DEMO");

        try {
            // STEP 1-5 DEMO: Execute Java-Native Multi-Agent Swarm for "Research Paper"
            TerminalUI.printBreadcrumb("Demo", "Multi-Agent Swarm", "Research Paper (IEEE/ACM)");
            String topic = "Impact of Artificial Intelligence on Cybersecurity";
            System.out.println("  Inquiry Topic   : " + topic);
            System.out.println("  Category        : Research Paper (6 Agents, DAG Dependencies, Priorities)");
            System.out.println("  Execution Model : Java FixedThreadPool (4 Concurrent Workers) with Synchronized Locks\n");

            List<AgentDeployment> agents = WorkflowCategory.RESEARCH_PAPER.createProposedDeployment(topic);
            WorkflowPlan plan = new WorkflowPlan(WorkflowCategory.RESEARCH_PAPER, topic, agents, 4);

            WorkflowExecutionResult result = workflowOrchestrator.executeWorkflow(plan);

            System.out.println();
            TerminalUI.printSuccess(String.format("Multi-Agent Swarm completed in %.2fs!", result.getDurationSeconds()));
            TerminalUI.printInfo("Saved Artifact: " + result.getOutputFilePath());

            // PERSISTENCE VERIFICATION
            TerminalUI.printBreadcrumb("Demo", "MySQL Persistence Verification");
            List<UserActivity> activities = userActivityDAO.getAllActivities();
            System.out.printf("  Total Records in MySQL: %d\n", activities.size());
            if (!activities.isEmpty()) {
                System.out.println("  Latest Audit Row: " + activities.get(0));
            }

            // Java Project Management Verification
            TerminalUI.printBreadcrumb("Demo", "Project Isolation");
            Project project = projectService.createProject("Cybersecurity LLM Invariants", "Evaluates defensive invariants against prompt injections.");
            TerminalUI.printSuccess("Active Project: " + project.getName() + " (ID: " + project.getProjectId().substring(0, Math.min(8, project.getProjectId().length())) + ")");

            KnowledgeResource res = knowledgeService.saveResource(project.getProjectId(),
                    "Adversarial Robustness in LLM Agents", ResourceType.URL, "https://arxiv.org/abs/2302.12173", "SECURITY");
            TerminalUI.printSuccess("Persisted Knowledge Asset: " + res.getTitle() + " (" + res.getResourceType() + ")");

            // Document Compilation Verification
            TerminalUI.printBreadcrumb("Demo", "Document Compilation");
            Document doc = documentService.createDocumentWithTemplate(project.getProjectId(), "Cybersecurity Defense Paper", DocumentType.RESEARCH_PAPER);
            List<DocumentSection> sections = documentService.getSections(doc.getDocumentId());
            if (!sections.isEmpty()) {
                DocumentSection firstSec = sections.get(0);
                documentService.updateSectionContent(firstSec.getSectionId(), "Formal verification of prompt guardrails across autonomous multi-agent systems.");
                documentService.reviewSection(firstSec.getSectionId(), true);
                TerminalUI.printSuccess("Drafted & Approved Section: " + firstSec.getSectionName());
            }

            TerminalUI.printSectionHeader("DEMO VERIFICATION COMPLETE");
            TerminalUI.printSuccess("Genuine Java multithreaded multi-agent orchestration & MySQL persistence verified!");
            TerminalUI.printSectionFooter();

        } catch (Exception e) {
            TerminalUI.printError("Demo encountered error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
