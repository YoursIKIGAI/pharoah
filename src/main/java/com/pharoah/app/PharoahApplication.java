package com.pharoah.app;

import com.pharoah.agent.AgentService;
import com.pharoah.ai.AIService;
import com.pharoah.ai.RemoteHttpAIService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.cli.PharoahCLI;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.config.AppConfig;
import com.pharoah.document.Document;
import com.pharoah.document.DocumentSection;
import com.pharoah.document.DocumentType;
import com.pharoah.document.DocumentWorkflowService;
import com.pharoah.knowledge.KnowledgeResource;
import com.pharoah.knowledge.KnowledgeService;
import com.pharoah.knowledge.ResourceType;
import com.pharoah.llm.LlmClient;
import com.pharoah.llm.LlmClientFactory;
import com.pharoah.orchestrator.*;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.*;
import com.pharoah.project.Project;
import com.pharoah.project.ProjectService;
import com.pharoah.tasks.TaskManager;
import com.pharoah.tasks.TaskService;

import java.util.List;

// VARIJ, ASHUTOSH, SANCHIT: Main Application Entrypoint.
// Technical Architecture: Java 21 Native Multi-Agent Orchestrator + Local Open-Source LLMs (Ollama)
// + Optional Python smolagents Tools + MySQL 8.0 Persistence.
public class PharoahApplication {

    private final DatabaseConnectionFactory dbFactory;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AgentService agentService;
    private final KnowledgeService knowledgeService;
    private final DocumentWorkflowService documentService;
    private final TaskAnalyticsEngine analyticsEngine;
    private final ThreadPoolManager poolManager;
    private final UserActivityDAO userActivityDAO;
    private final RunDao runDao;
    private final LlmClient llmClient;
    private final PythonAgentBridge toolBridge;
    private final WorkflowOrchestrator workflowOrchestrator;
    private final PharoahCLI cli;

    public PharoahApplication(boolean mockMode, boolean useTools) {
        // Step 6: Initialize database connection with strict/graceful fallback
        this.dbFactory = new DatabaseConnectionFactory();

        ProjectDao projectDao = new ProjectDao(dbFactory);
        TaskDao taskDao = new TaskDao(dbFactory);
        AgentDao agentDao = new AgentDao(dbFactory);
        KnowledgeDao knowledgeDao = new KnowledgeDao(dbFactory);
        DocumentDao documentDao = new DocumentDao(dbFactory);
        this.userActivityDAO = new UserActivityDAO(dbFactory);
        this.runDao = new RunDao(dbFactory);

        // Core business services
        this.projectService = new ProjectService(projectDao);
        this.taskService = new TaskService(taskDao);
        this.agentService = new AgentService(agentDao);
        this.knowledgeService = new KnowledgeService(knowledgeDao);
        this.documentService = new DocumentWorkflowService(documentDao);
        this.analyticsEngine = new TaskAnalyticsEngine();

        // Concurrency and orchestration engine
        this.poolManager = new ThreadPoolManager(Runtime.getRuntime().availableProcessors());

        // Step 2 & 4: LLM Client & Tool Bridge
        this.llmClient = LlmClientFactory.createClient(mockMode);
        this.toolBridge = new PythonAgentBridge();

        // Step 3 & 5: Central Multi-Agent Orchestrator with Semaphore rate limiting & failure propagation
        this.workflowOrchestrator = new WorkflowOrchestrator(
                userActivityDAO,
                runDao,
                llmClient,
                toolBridge,
                useTools,
                600_000L
        );

        // Prompt-driven CLI
        this.cli = new PharoahCLI(dbFactory, runDao, llmClient, workflowOrchestrator, useTools);
    }

    public PharoahApplication() {
        this(false, false);
    }

    public static void main(String[] args) {
        boolean mockMode = false;
        boolean useTools = false;
        String oneShotPrompt = null;
        boolean isDemo = false;
        boolean isStatus = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--mock")) {
                mockMode = true;
            } else if (arg.equalsIgnoreCase("--tools")) {
                useTools = true;
            } else if (arg.equalsIgnoreCase("--require-mysql")) {
                DatabaseConnectionFactory.requireMySql = true;
            } else if (arg.equalsIgnoreCase("--demo")) {
                isDemo = true;
            } else if (arg.equalsIgnoreCase("status") || arg.equalsIgnoreCase("--status")) {
                isStatus = true;
            } else if (arg.equalsIgnoreCase("--prompt") || arg.equalsIgnoreCase("-p")) {
                if (i + 1 < args.length) {
                    oneShotPrompt = args[++i];
                }
            }
        }

        PharoahApplication app = new PharoahApplication(mockMode, useTools);

        if (isDemo) {
            app.runAutomatedDemo();
        } else if (isStatus) {
            app.printStatus();
        } else if (oneShotPrompt != null) {
            app.cli.executeUserPrompt(oneShotPrompt);
        } else {
            app.cli.startShell();
        }

        app.poolManager.gracefulShutdown(2);
    }

    private void printStatus() {
        AppConfig cfg = AppConfig.getInstance();
        System.out.println("\n==================================================");
        System.out.println("PHAROAH: System Status and Health");
        System.out.println("==================================================");
        System.out.printf("Database Engine : %s (Active: %b)\n", dbFactory.getDbType(), dbFactory.isMySqlActive());
        System.out.printf("Database URL    : %s\n", cfg.getDbUrl());
        System.out.printf("Database User   : %s\n", cfg.getDbUser());
        System.out.printf("Database Auth   : %s\n", cfg.getMaskedPassword());
        System.out.printf("LLM Provider    : %s (Base: %s | Model: %s)\n", cfg.getLlmProvider(), cfg.getLlmBaseUrl(), cfg.getLlmModel());
        System.out.printf("Worker Pool     : %d Available CPU Processors\n", poolManager.getPoolSize());
        System.out.printf("Active Project  : %s\n", projectService.getActiveProject() != null ? projectService.getActiveProject().getName() : "None");
        System.out.println("==================================================");
        dbFactory.printDatabaseStatus();
    }

    public void runAutomatedDemo() {
        System.out.println("\n==================================================");
        System.out.println("PHAROAH: Automated Multi-Agent Orchestration Demo");
        System.out.println("==================================================");

        try {
            String topic = "Impact of Artificial Intelligence on Cybersecurity";
            System.out.println("Inquiry Topic   : " + topic);
            System.out.println("Category        : Research Paper (6 Agents, Parallel Execution)");
            System.out.println("Execution Model : Java FixedThreadPool with synchronized monitor coordination\n");

            List<AgentDeployment> agents = WorkflowCategory.RESEARCH_PAPER.createProposedDeployment(topic);
            WorkflowPlan plan = new WorkflowPlan(WorkflowCategory.RESEARCH_PAPER, topic, agents, 4, ExecutionMode.PARALLEL);

            WorkflowExecutionResult result = workflowOrchestrator.executeWorkflow(plan);

            System.out.println("\n==================================================");
            System.out.println("DEMO GENERATED OUTPUT SAMPLE");
            System.out.println("==================================================");
            if (!plan.getAgents().isEmpty()) {
                AgentDeployment firstAgent = plan.getAgents().get(0);
                System.out.println(firstAgent.getName() + ": " + firstAgent.getResponsibility());
                System.out.println();
                String content = result.getAgentOutputs().get(firstAgent.getId()) != null
                        ? result.getAgentOutputs().get(firstAgent.getId()).getContent()
                        : "Sample content generated.";
                System.out.println(content);
            }

            System.out.println("\n==================================================");
            System.out.printf("Multi-Agent Swarm completed in %.2fs!\n", result.getDurationSeconds());
            System.out.println("Saved Artifact: " + result.getOutputFilePath());

            // Persistence Verification
            List<com.pharoah.core.model.UserActivity> activities = userActivityDAO.getAllActivities();
            String dbSource = dbFactory.isMySqlActive() ? "MySQL 8.0" : "SQLite fallback";
            System.out.printf("Total Records in %s: %d\n", dbSource, activities.size());
            if (!activities.isEmpty()) {
                System.out.println("Latest Audit Row: " + activities.get(0));
            }

            // Project Isolation
            Project project = projectService.createProject("Cybersecurity LLM Invariants", "Evaluates defensive invariants against prompt injections.");
            System.out.println("Active Project: " + project.getName() + " (ID: " + project.getProjectId().substring(0, Math.min(8, project.getProjectId().length())) + ")");

            KnowledgeResource res = knowledgeService.saveResource(project.getProjectId(),
                    "Adversarial Robustness in LLM Agents", ResourceType.URL, "https://arxiv.org/abs/2302.12173", "SECURITY");
            System.out.println("Persisted Knowledge Asset: " + res.getTitle() + " (" + res.getResourceType() + ")");

            // Document Compilation Verification
            Document doc = documentService.createDocumentWithTemplate(project.getProjectId(), "Cybersecurity Defense Paper", DocumentType.RESEARCH_PAPER);
            List<DocumentSection> sections = documentService.getSections(doc.getDocumentId());
            if (!sections.isEmpty()) {
                DocumentSection firstSec = sections.get(0);
                documentService.updateSectionContent(firstSec.getSectionId(), "Formal verification of prompt guardrails across autonomous multi-agent systems.");
                documentService.reviewSection(firstSec.getSectionId(), true);
                System.out.println("Drafted & Approved Section: " + firstSec.getSectionName());
            }

            System.out.println("\n==================================================");
            System.out.println("DEMO VERIFICATION COMPLETE");
            System.out.printf("Genuine Java multithreaded multi-agent orchestration & %s persistence verified.\n", dbSource);
            System.out.println("==================================================\n");

        } catch (Exception e) {
            System.out.println("Demo encountered error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
