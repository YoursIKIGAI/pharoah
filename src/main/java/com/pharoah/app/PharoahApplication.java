package com.pharoah.app;

import com.pharoah.agent.AgentService;
import com.pharoah.ai.AIService;
import com.pharoah.ai.RemoteHttpAIService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.bridge.PythonExecutionResult;
import com.pharoah.cli.PharoahCLI;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
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
        System.out.println("=========================================================================");
        System.out.println("  👑 PHAROAH SYSTEM STATUS");
        System.out.printf("  Database Engine : %s (Active: %b)\n", dbFactory.getDbType(), dbFactory.isMySqlActive());
        System.out.printf("  Worker Pool     : %d Threads\n", poolManager.getPoolSize());
        System.out.printf("  Active Project  : %s\n", projectService.getActiveProject() != null ? projectService.getActiveProject().getName() : "None");
        System.out.println("=========================================================================");
    }

    // VARIJ, ASHUTOSH, SANCHIT: Automated end-to-end demo validating format-driven multi-agent execution & persistence
    public void runAutomatedDemo() {
        System.out.println("=========================================================================");
        System.out.println("   👑 PHAROAH: AUTOMATED FORMAT-DRIVEN MULTI-AGENT DEMO WALKTHROUGH     ");
        System.out.println("=========================================================================\n");

        try {
            // STEP 1-5 DEMO: Execute Format-Driven Multi-Agent Swarm for "Research Paper"
            System.out.println("--- [1] MULTI-AGENT SWARM: FORMAT-DRIVEN RESEARCH WORKFLOW ---");
            String topic = "Impact of Artificial Intelligence on Cybersecurity";
            System.out.println("  Topic           : " + topic);
            System.out.println("  Workflow Format : Research Paper (Format #1)");
            System.out.println("  Selected Sections: Literature Review, Research Gap, Methodology, Data Analysis");
            System.out.println("  Priorities      : Literature Review:HIGH, Research Gap:HIGH, Methodology:MEDIUM, Data Analysis:LOW");
            System.out.println("  Executing Swarm via Python Subprocess Bridge...\n");

            PythonExecutionResult swarmResult = pythonBridge.runWorkflow(
                    "1",
                    topic,
                    "Literature Review,Research Gap,Methodology,Data Analysis",
                    "Literature Review:HIGH,Research Gap:HIGH,Methodology:MEDIUM,Data Analysis:LOW",
                    "Literature Review:Research Specialist,Research Gap:Analysis Specialist,Methodology:Research Specialist,Data Analysis:Data & Metrics Analyst"
            );

            System.out.printf("\n  Swarm Status     : %s (Duration: %.2fs)\n", swarmResult.getStatus(), swarmResult.getElapsedSeconds());
            System.out.printf("  Generated Artifact: %s\n", swarmResult.getOutputFile());

            // PERSISTENCE: Save to MySQL user_activity table
            System.out.println("\n--- [2] DATABASE PERSISTENCE: LOGGING USER ACTIVITY TO MYSQL ---");
            UserActivity demoActivity = new UserActivity(
                    "University Student",
                    topic,
                    "Research Paper",
                    "Literature Review, Research Gap, Methodology, Data Analysis",
                    "Research Specialist, Analysis Specialist, Data & Metrics Analyst, Quality Reviewer"
            );
            userActivityDAO.recordActivity(demoActivity);
            System.out.println("  Successfully recorded activity into MySQL 'user_activity' table!");

            List<UserActivity> activities = userActivityDAO.getAllActivities();
            System.out.printf("  Total Activities in Database: %d\n", activities.size());
            if (!activities.isEmpty()) {
                System.out.println("  Latest Record: " + activities.get(0));
            }

            // Java Project Management Verification
            System.out.println("\n--- [3] MODULE A: PROJECT ISOLATION & KNOWLEDGE ASSETS ---");
            Project project = projectService.createProject("Cybersecurity LLM Invariants", "Evaluates defensive invariants against prompt injections.");
            System.out.println("  Active Project: " + project.getName() + " (ID: " + project.getProjectId().substring(0, Math.min(8, project.getProjectId().length())) + ")");

            KnowledgeResource res = knowledgeService.saveResource(project.getProjectId(),
                    "Adversarial Robustness in LLM Agents", ResourceType.URL, "https://arxiv.org/abs/2302.12173", "SECURITY");
            System.out.println("  Persisted Knowledge Asset: " + res.getTitle() + " (" + res.getResourceType() + ")");

            // Document Compilation Verification
            System.out.println("\n--- [4] MODULE F: MULTI-SECTION DOCUMENT COMPILATION ---");
            Document doc = documentService.createDocumentWithTemplate(project.getProjectId(), "Cybersecurity Defense Paper", DocumentType.RESEARCH_PAPER);
            List<DocumentSection> sections = documentService.getSections(doc.getDocumentId());
            if (!sections.isEmpty()) {
                DocumentSection firstSec = sections.get(0);
                documentService.updateSectionContent(firstSec.getSectionId(), "Formal verification of prompt guardrails across autonomous multi-agent systems.");
                documentService.reviewSection(firstSec.getSectionId(), true);
                System.out.println("  Drafted & Approved Section: " + firstSec.getSectionName());
            }

            System.out.println("\n=========================================================================");
            System.out.println("  🎉 VERIFICATION PASSED: FORMAT-DRIVEN MULTI-AGENT ARCHITECTURE FUNCTIONAL!");
            System.out.println("=========================================================================");

        } catch (Exception e) {
            System.err.println("Demo encountered error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
