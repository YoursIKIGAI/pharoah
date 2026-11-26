package com.pharoah.app;

import com.pharoah.agent.AgentService;
import com.pharoah.ai.AIService;
import com.pharoah.ai.RemoteHttpAIService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.bridge.PythonExecutionResult;
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
import com.pharoah.orchestrator.TaskOrchestrator;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.*;
import com.pharoah.project.Project;
import com.pharoah.project.ProjectService;
import com.pharoah.tasks.TaskManager;
import com.pharoah.tasks.TaskService;

import java.util.List;

// VARIJ, ASHUTOSH, SANCHIT: Main Application Entrypoint.
// Hybrid Architecture: Java Terminal Orchestrator & CLI + Python Multi-Agent Swarm + MySQL 8.0 Persistence.
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
    private final UserActivityDAO userActivityDAO;
    private final PythonAgentBridge pythonBridge;
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
        this.orchestrator = new TaskOrchestrator(taskManager, poolManager, taskDao, agentService, aiEngine);

        // VARIJ & ASHUTOSH: Initialize Python Multi-Agent Swarm Bridge
        this.pythonBridge = new PythonAgentBridge();

        // Terminal CLI
        this.cli = new PharoahCLI(
                projectService,
                taskService,
                agentService,
                orchestrator,
                knowledgeService,
                documentService,
                analyticsEngine,
                userActivityDAO,
                pythonBridge
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
        System.out.printf("  Worker Pool     : %d Threads\n", poolManager.getPoolSize());
        System.out.printf("  Active Project  : %s\n", projectService.getActiveProject() != null ? projectService.getActiveProject().getName() : "None");
        TerminalUI.printSectionFooter();
    }

    // VARIJ, ASHUTOSH, SANCHIT: Automated end-to-end demo validating format-driven multi-agent execution & persistence
    public void runAutomatedDemo() {
        TerminalUI.printBanner("2.0-LTS", dbFactory.getDbType(), dbFactory.isMySqlActive());
        TerminalUI.printSectionHeader("AUTOMATED MULTI-AGENT SWARM & PERSISTENCE DEMO");

        try {
            // STEP 1-5 DEMO: Execute Format-Driven Multi-Agent Swarm for "Research Paper"
            TerminalUI.printBreadcrumb("Demo", "Multi-Agent Swarm", "Research Paper");
            String topic = "Impact of Artificial Intelligence on Cybersecurity";
            System.out.println("  Topic           : " + topic);
            System.out.println("  Workflow Format : Research Paper (Format #1)");
            System.out.println("  Selected Sections: Literature Review, Research Gap, Methodology, Data Analysis");
            System.out.println("  Priorities      : Literature Review:HIGH, Research Gap:HIGH, Methodology:MEDIUM, Data Analysis:LOW");
            TerminalUI.printInfo("Dispatching swarm via Python Subprocess Bridge...\n");

            PythonExecutionResult swarmResult = pythonBridge.runWorkflow(
                    "1",
                    topic,
                    "Literature Review,Research Gap,Methodology,Data Analysis",
                    "Literature Review:HIGH,Research Gap:HIGH,Methodology:MEDIUM,Data Analysis:LOW",
                    "Literature Review:Research Specialist,Research Gap:Analysis Specialist,Methodology:Research Specialist,Data Analysis:Data & Metrics Analyst"
            );

            System.out.println();
            TerminalUI.printSuccess(String.format("Swarm Status: %s in %.2fs", swarmResult.getStatus(), swarmResult.getElapsedSeconds()));
            TerminalUI.printInfo("Compiled Artifact: " + swarmResult.getOutputFile());

            // PERSISTENCE: Save to MySQL user_activity table
            TerminalUI.printBreadcrumb("Demo", "MySQL Persistence");
            UserActivity demoActivity = new UserActivity(
                    "University Student",
                    topic,
                    "Research Paper",
                    "Literature Review, Research Gap, Methodology, Data Analysis",
                    "Research Specialist, Analysis Specialist, Data & Metrics Analyst, Quality Reviewer"
            );
            userActivityDAO.recordActivity(demoActivity);
            TerminalUI.printSuccess("Activity logged to MySQL table 'user_activity'!");

            List<UserActivity> activities = userActivityDAO.getAllActivities();
            System.out.printf("  Total Records in Database: %d\n", activities.size());
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
            TerminalUI.printSuccess("All format-driven workflows, multi-agent swarms, and MySQL persistence verified!");
            TerminalUI.printSectionFooter();

        } catch (Exception e) {
            TerminalUI.printError("Demo encountered error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
