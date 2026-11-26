package com.pharoah.cli;

import com.pharoah.agent.AgentConfig;
import com.pharoah.agent.AgentRole;
import com.pharoah.agent.AgentService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.bridge.PythonExecutionResult;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
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
import com.pharoah.persistence.dao.UserActivityDAO;
import com.pharoah.project.Project;
import com.pharoah.project.ProjectService;
import com.pharoah.tasks.TaskService;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;

// VARIJ & SANCHIT: Refined Terminal Controller.
// Delivers a clean, polished command-line experience with breadcrumb navigation,
// consistent status indicators, format-driven workflows, and zero raw technical dumps.
public class PharoahCLI {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final AgentService agentService;
    private final TaskOrchestrator orchestrator;
    private final KnowledgeService knowledgeService;
    private final DocumentWorkflowService documentService;
    private final TaskAnalyticsEngine analyticsEngine;
    private final UserActivityDAO userActivityDAO;
    private final PythonAgentBridge pythonBridge;
    private final Scanner scanner;

    public PharoahCLI(ProjectService projectService,
                      TaskService taskService,
                      AgentService agentService,
                      TaskOrchestrator orchestrator,
                      KnowledgeService knowledgeService,
                      DocumentWorkflowService documentService,
                      TaskAnalyticsEngine analyticsEngine,
                      UserActivityDAO userActivityDAO,
                      PythonAgentBridge pythonBridge) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.agentService = agentService;
        this.orchestrator = orchestrator;
        this.knowledgeService = knowledgeService;
        this.documentService = documentService;
        this.analyticsEngine = analyticsEngine;
        this.userActivityDAO = userActivityDAO;
        this.pythonBridge = (pythonBridge != null) ? pythonBridge : new PythonAgentBridge();
        this.scanner = new Scanner(System.in);
    }

    public PharoahCLI(ProjectService projectService,
                      TaskService taskService,
                      AgentService agentService,
                      TaskOrchestrator orchestrator,
                      KnowledgeService knowledgeService,
                      DocumentWorkflowService documentService,
                      TaskAnalyticsEngine analyticsEngine) {
        this(projectService, taskService, agentService, orchestrator, knowledgeService, documentService, analyticsEngine, null, new PythonAgentBridge());
    }

    // VARIJ: Main interactive terminal shell
    public void startShell() {
        boolean running = true;
        while (running) {
            TerminalUI.printBanner("2.0-LTS", "MySQL 8.0", userActivityDAO != null);

            TerminalUI.printSectionHeader("SELECT WORKFLOW OR ACTION");
            TerminalUI.printMenuOption("1", "Research Paper", "Academic format with literature review & gap analysis");
            TerminalUI.printMenuOption("2", "Research Analysis", "Deep analytical inquiry and empirical evaluation");
            TerminalUI.printMenuOption("3", "Report Generation", "Executive summaries and actionable recommendations");
            TerminalUI.printMenuOption("4", "Blog Writing", "High-impact prose with practical code examples");
            TerminalUI.printMenuOption("5", "Technical Docs", "Architecture, API references, and setup guides");
            TerminalUI.printMenuOption("6", "Business Proposal", "Market problem, ROI forecast, and execution plan");
            TerminalUI.printMenuOption("7", "Data Analysis", "Statistical metrics and benchmark distributions");
            TerminalUI.printMenuOption("8", "Custom Workflow", "Define custom sections and dynamic agent swarm");
            TerminalUI.printDivider();
            TerminalUI.printMenuOption("9", "Audit Log (MySQL)", "View persistent activity history from MySQL database");
            TerminalUI.printMenuOption("10", "Advanced Tools", "Projects, task graphs, knowledge repository");
            TerminalUI.printMenuOption("0", "Exit PHAROAH", "Clean shutdown and resource release");
            TerminalUI.printSectionFooter();

            System.out.print("  Select an option (0-10): ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1", "2", "3", "4", "5", "6", "7", "8" -> handleFormatDrivenWorkflow(choice);
                    case "9" -> handleViewActivityLog();
                    case "10" -> handleAdvancedMenu();
                    case "0", "exit", "quit" -> {
                        TerminalUI.printSuccess("Shutting down cleanly. All persistent data preserved in MySQL.");
                        running = false;
                    }
                    default -> TerminalUI.printWarning("Invalid choice. Please select an option between 0 and 10.");
                }
            } catch (Exception e) {
                TerminalUI.printError("An error occurred: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 5-STEP FORMAT-DRIVEN MULTI-AGENT WORKFLOW
    // =========================================================================

    private void handleFormatDrivenWorkflow(String choice) {
        String workflowName = switch (choice) {
            case "2" -> "Research Analysis";
            case "3" -> "Report Generation";
            case "4" -> "Blog Writing";
            case "5" -> "Technical Documentation";
            case "6" -> "Business Proposal";
            case "7" -> "Data Analysis";
            case "8" -> "Custom Workflow";
            default -> "Research Paper";
        };

        // STEP 1 & 2: TOPIC SCOPING
        TerminalUI.printBreadcrumb(workflowName, "Topic Scoping");
        System.out.println("  Enter your target topic or research inquiry:");
        System.out.print("  > ");
        String topic = scanner.nextLine().trim();
        if (topic.isEmpty()) {
            topic = "Impact of Artificial Intelligence on Cybersecurity";
            TerminalUI.printInfo("Default topic selected: " + topic);
        }

        // STEP 3: DOCUMENT STRUCTURE & SECTION SELECTION
        TerminalUI.printBreadcrumb(workflowName, "Section Selection");
        List<String> availableSections = getStandardSectionsForWorkflow(choice);

        if ("8".equals(choice)) {
            System.out.println("  Enter custom section names separated by commas (e.g. Scope, Design, Tests):");
            System.out.print("  > ");
            String customIn = scanner.nextLine().trim();
            if (!customIn.isEmpty()) {
                availableSections = Arrays.stream(customIn.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        }

        TerminalUI.printSectionHeader(workflowName.toUpperCase() + " SECTIONS");
        for (int i = 0; i < availableSections.size(); i++) {
            String sec = availableSections.get(i);
            String recAgent = getRecommendedAgentForSection(sec);
            String defPrio = getDefaultPriorityForSection(sec);
            System.out.printf("  %2d. %-24s │ Agent: %-22s │ Prio: %s\n", i + 1, sec, recAgent, defPrio);
        }
        TerminalUI.printSectionFooter();

        System.out.println("  Choose sections to execute:");
        System.out.println("    [A] Complete Document (All sections)");
        System.out.println("    Or enter comma-separated numbers (e.g. 3, 4, 5, 6):");
        System.out.print("  > ");
        String secInput = scanner.nextLine().trim();

        List<String> selectedSections = new ArrayList<>();
        if (secInput.equalsIgnoreCase("A") || secInput.isEmpty()) {
            selectedSections.addAll(availableSections);
        } else {
            for (String tok : secInput.split(",")) {
                try {
                    int idx = Integer.parseInt(tok.trim()) - 1;
                    if (idx >= 0 && idx < availableSections.size()) {
                        selectedSections.add(availableSections.get(idx));
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (selectedSections.isEmpty()) {
                selectedSections.addAll(availableSections);
            }
        }

        // STEP 4: PRIORITY CONFIGURATION
        TerminalUI.printBreadcrumb(workflowName, "Priority Scheduling");
        Map<String, String> priorities = new LinkedHashMap<>();
        for (String s : selectedSections) {
            priorities.put(s, getDefaultPriorityForSection(s));
        }

        TerminalUI.printSectionHeader("SECTION EXECUTION SEQUENCE & PRIORITIES");
        for (String s : selectedSections) {
            System.out.printf("   • %-26s : [%s]\n", s, priorities.get(s));
        }
        TerminalUI.printSectionFooter();

        System.out.print("  Customize priorities? (y/N) [default N]: ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            for (String s : selectedSections) {
                System.out.printf("    Priority for '%s' ([H]igh, [M]edium, [L]ow) [current %s]: ", s, priorities.get(s));
                String pIn = scanner.nextLine().trim().toUpperCase();
                if (pIn.startsWith("H")) priorities.put(s, "HIGH");
                else if (pIn.startsWith("L")) priorities.put(s, "LOW");
                else if (pIn.startsWith("M")) priorities.put(s, "MEDIUM");
            }
        }

        // STEP 5: AGENT ALLOCATION
        TerminalUI.printBreadcrumb(workflowName, "Agent Swarm Allocation");
        Map<String, String> agentAssignments = new LinkedHashMap<>();
        for (String s : selectedSections) {
            agentAssignments.put(s, getRecommendedAgentForSection(s));
        }

        TerminalUI.printSectionHeader("RECOMMENDED AGENT ALLOCATIONS");
        for (String s : selectedSections) {
            System.out.printf("   • %-26s ➔ %s\n", s, agentAssignments.get(s));
        }
        System.out.printf("   • %-26s ➔ %s\n", "Quality & Verification Gate", "Quality Reviewer");
        TerminalUI.printSectionFooter();

        System.out.print("  Start multi-agent swarm execution? (Y/n): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("n")) {
            TerminalUI.printWarning("Workflow cancelled.");
            return;
        }

        // LIVE EXECUTION STREAMING
        TerminalUI.printBreadcrumb(workflowName, "Live Swarm Execution");
        TerminalUI.printInfo("Launching autonomous multi-agent swarm...");
        System.out.println();

        String sectionsParam = String.join(",", selectedSections);
        StringBuilder prioParam = new StringBuilder();
        priorities.forEach((k, v) -> prioParam.append(k).append(":").append(v).append(","));
        StringBuilder agentParam = new StringBuilder();
        agentAssignments.forEach((k, v) -> agentParam.append(k).append(":").append(v).append(","));

        PythonExecutionResult result = pythonBridge.runWorkflow(
                choice,
                topic,
                sectionsParam,
                prioParam.toString(),
                agentParam.toString()
        );

        // PERSISTENCE CONFIRMATION
        TerminalUI.printBreadcrumb(workflowName, "Persistence & Artifacts");
        if (result.isSuccess()) {
            TerminalUI.printSuccess("All sections generated, reviewed, and approved!");
            if (result.getOutputFile() != null && !result.getOutputFile().isEmpty()) {
                TerminalUI.printInfo("Compiled Document Artifact: " + result.getOutputFile());
            }

            if (userActivityDAO != null) {
                String sectionsWorked = String.join(", ", selectedSections);
                String agentsUsed = (result.getAgentsUsed() != null && !result.getAgentsUsed().isEmpty())
                        ? String.join(", ", result.getAgentsUsed())
                        : String.join(", ", new HashSet<>(agentAssignments.values()));

                UserActivity activity = new UserActivity("Student", topic, workflowName, sectionsWorked, agentsUsed);
                userActivityDAO.recordActivity(activity);
                TerminalUI.printSuccess("Execution audit successfully recorded in MySQL table 'pharoah_db.user_activity'.");
            }
        } else {
            TerminalUI.printError("Swarm execution encountered an error.");
        }

        System.out.print("\n  Press Enter to return to main menu...");
        scanner.nextLine();
    }

    // =========================================================================
    // MYSQL ACTIVITY LOG AUDIT
    // =========================================================================

    private void handleViewActivityLog() {
        TerminalUI.printBreadcrumb("MySQL Activity History");

        if (userActivityDAO == null) {
            TerminalUI.printWarning("Database connection is offline. Showing session history only.");
            return;
        }

        List<UserActivity> activities = userActivityDAO.getAllActivities();
        if (activities.isEmpty()) {
            TerminalUI.printInfo("No previous activities recorded in MySQL yet. Run any workflow to log execution.");
        } else {
            TerminalUI.printSectionHeader(String.format("PERSISTENT ACTIVITY AUDIT TRAIL (%d RECORDS)", activities.size()));
            for (UserActivity a : activities) {
                System.out.printf("  #%-3d │ %-20s │ %-28s │ %s\n",
                        a.getActivityId(), a.getWorkflowType(),
                        a.getTopic().length() > 28 ? a.getTopic().substring(0, 25) + "..." : a.getTopic(),
                        a.getStatus());
                System.out.printf("       └─ Sections : %s\n", a.getSectionsWorked());
                System.out.printf("       └─ Agents   : %s\n", a.getAgentsUsed());
                System.out.println();
            }
            TerminalUI.printSectionFooter();
        }

        System.out.print("  Press Enter to return to main menu...");
        scanner.nextLine();
    }

    // =========================================================================
    // ADVANCED MODULES SUB-MENU
    // =========================================================================

    private void handleAdvancedMenu() throws Exception {
        boolean inSub = true;
        while (inSub) {
            Project active = projectService.getActiveProject();
            String activeName = (active != null) ? active.getName() : "None";

            TerminalUI.printBreadcrumb("Advanced Modules", "Active Project: " + activeName);
            TerminalUI.printSectionHeader("ADVANCED ENGINEERING & KNOWLEDGE MODULES");
            TerminalUI.printMenuOption("1", "Project Workspaces", "Create, switch, and isolate project domains");
            TerminalUI.printMenuOption("2", "Task Manager", "Create micro-tasks, inspect dependencies & human review");
            TerminalUI.printMenuOption("3", "Agent Registry", "Inspect and register agent profiles");
            TerminalUI.printMenuOption("4", "Task Decomposition", "Decompose objectives via Java concurrency");
            TerminalUI.printMenuOption("5", "Knowledge Repository", "Store URLs, research notes, and code snippets");
            TerminalUI.printMenuOption("6", "Stream Analytics", "Stream API groupings and completion analytics");
            TerminalUI.printMenuOption("0", "Return", "Back to Main Workflow Selector");
            TerminalUI.printSectionFooter();

            System.out.print("  Select advanced option (0-6): ");
            String subChoice = scanner.nextLine().trim();
            switch (subChoice) {
                case "1" -> handleProjectMenu();
                case "2" -> handleTaskMenu();
                case "3" -> handleAgentMenu();
                case "4" -> handleOrchestrationMenu();
                case "5" -> handleKnowledgeMenu();
                case "6" -> handleAnalyticsMenu();
                case "0", "back" -> inSub = false;
                default -> TerminalUI.printWarning("Invalid option.");
            }
        }
    }

    private void handleProjectMenu() throws SQLException {
        TerminalUI.printBreadcrumb("Advanced Modules", "Project Workspaces");
        System.out.println("  1. List All Projects");
        System.out.println("  2. Create New Project");
        System.out.println("  3. Switch Active Project");
        System.out.println("  4. Delete Project");
        System.out.print("  Choose (1-4): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                List<Project> list = projectService.listAllProjects();
                System.out.printf("\n  Total Projects (%d):\n", list.size());
                for (Project p : list) {
                    boolean isCurr = projectService.getActiveProject() != null && projectService.getActiveProject().getProjectId().equals(p.getProjectId());
                    System.out.printf("   %s %s\n", isCurr ? "➔ [ACTIVE]" : "  [      ]", p);
                }
            }
            case "2" -> {
                System.out.print("  Enter Project Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("  Enter Description: ");
                String desc = scanner.nextLine().trim();
                Project p = projectService.createProject(name, desc);
                TerminalUI.printSuccess("Project created and activated: " + p.getName());
            }
            case "3" -> {
                List<Project> list = projectService.listAllProjects();
                for (int i = 0; i < list.size(); i++) {
                    System.out.printf("   [%d] %s\n", i + 1, list.get(i).getName());
                }
                System.out.print("  Select project number: ");
                try {
                    int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    if (idx >= 0 && idx < list.size()) {
                        projectService.setActiveProject(list.get(idx).getProjectId());
                        TerminalUI.printSuccess("Switched to project: " + list.get(idx).getName());
                    }
                } catch (Exception ignored) {}
            }
            case "4" -> {
                System.out.print("  Enter Project ID to delete: ");
                String pid = scanner.nextLine().trim();
                if (projectService.deleteProject(pid)) {
                    TerminalUI.printSuccess("Project deleted.");
                } else {
                    TerminalUI.printWarning("Project not found.");
                }
            }
        }
    }

    private void handleTaskMenu() throws SQLException {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        TerminalUI.printBreadcrumb("Advanced Modules", "Task Management");
        System.out.println("  1. List Tasks in Current Project");
        System.out.println("  2. Create New Task");
        System.out.println("  3. Review & Approve Task (Human-in-the-Loop)");
        System.out.println("  4. Delete Task");
        System.out.print("  Choose (1-4): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                List<Task> tasks = taskService.getTasksByProject(pid);
                System.out.printf("\n  Tasks in Project (%d):\n", tasks.size());
                for (Task t : tasks) {
                    System.out.printf("   • [%s] #%s : %s\n", t.getStatus(), t.getTaskId().substring(0, Math.min(8, t.getTaskId().length())), t.getTitle());
                }
            }
            case "2" -> {
                System.out.print("  Task Title: ");
                String title = scanner.nextLine().trim();
                System.out.print("  Instructions: ");
                String payload = scanner.nextLine().trim();
                Task t = taskService.createTask(pid, title, TaskType.RESEARCH, Priority.HIGH, payload, null, null);
                TerminalUI.printSuccess("Task created: " + t.getTitle());
            }
            case "3" -> {
                System.out.print("  Enter Task ID: ");
                String tid = scanner.nextLine().trim();
                Task t = taskService.getTaskById(tid);
                if (t != null) {
                    System.out.println("  Current Output:\n" + t.getFinalOutput());
                    System.out.print("  Approve output? (y/N): ");
                    boolean approved = scanner.nextLine().trim().equalsIgnoreCase("y");
                    taskService.recordReviewApproval(tid, approved, "Reviewed via terminal");
                    TerminalUI.printSuccess("Task review recorded.");
                } else {
                    TerminalUI.printWarning("Task not found.");
                }
            }
            case "4" -> {
                System.out.print("  Enter Task ID to delete: ");
                taskService.deleteTask(scanner.nextLine().trim());
                TerminalUI.printSuccess("Task deleted.");
            }
        }
    }

    private void handleAgentMenu() throws SQLException {
        TerminalUI.printBreadcrumb("Advanced Modules", "Agent Registry");
        System.out.println("  1. List Registered Agents");
        System.out.println("  2. Register Configurable Agent");
        System.out.print("  Choose (1-2): ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            List<AgentConfig> agents = agentService.listAllAgents();
            for (AgentConfig a : agents) {
                System.out.printf("   • %-24s | Role: %-20s | Active: %b\n", a.getName(), a.getRole(), a.isActive());
            }
        } else if ("2".equals(sub)) {
            System.out.print("  Agent Name: ");
            String name = scanner.nextLine().trim();
            AgentConfig created = agentService.registerAgent(name, AgentRole.RESEARCH_AGENT, "Custom guidelines", "MOCK_HEURISTIC");
            TerminalUI.printSuccess("Agent registered: " + created.getName());
        }
    }

    private void handleOrchestrationMenu() throws Exception {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        TerminalUI.printBreadcrumb("Advanced Modules", "Java Orchestrator");
        System.out.print("  Enter high-level objective: ");
        String title = scanner.nextLine().trim();
        System.out.print("  Enter domain topic: ");
        String topic = scanner.nextLine().trim();

        List<Task> subtasks = orchestrator.decomposeObjective(pid, title, topic);
        TerminalUI.printSuccess(String.format("Decomposed into %d subtasks with dependency graph.", subtasks.size()));
    }

    private void handleKnowledgeMenu() throws SQLException {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        TerminalUI.printBreadcrumb("Advanced Modules", "Knowledge Repository");
        System.out.println("  1. Save Knowledge Asset (URL / Code / Note)");
        System.out.println("  2. Search Knowledge Repository");
        System.out.print("  Choose (1-2): ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            System.out.print("  Asset Title: ");
            String title = scanner.nextLine().trim();
            System.out.print("  Content or URL: ");
            String content = scanner.nextLine().trim();
            System.out.print("  Topic Tag: ");
            String tag = scanner.nextLine().trim().toUpperCase();
            KnowledgeResource r = knowledgeService.saveResource(pid, title, ResourceType.URL, content, tag);
            TerminalUI.printSuccess("Knowledge asset persisted: " + r.getTitle());
        } else if ("2".equals(sub)) {
            System.out.print("  Search keyword: ");
            String kw = scanner.nextLine().trim();
            List<KnowledgeResource> list = knowledgeService.searchKnowledge(kw, "");
            for (KnowledgeResource kr : list) {
                System.out.printf("   • [%s] %s (%s)\n", kr.getResourceType(), kr.getTitle(), kr.getContentOrUrl());
            }
        }
    }

    private void handleAnalyticsMenu() throws SQLException {
        TerminalUI.printBreadcrumb("Advanced Modules", "Stream API Analytics");
        List<Task> tasks = taskService.getAllTasks();
        System.out.printf("  Total Tracked Tasks: %d\n", tasks.size());
        if (!tasks.isEmpty()) {
            System.out.println("\n  1. Workload by Category (Collectors.groupingBy):");
            analyticsEngine.countTasksByType(tasks).forEach((t, count) ->
                    System.out.printf("     %-20s : %d tasks\n", t, count));
        }
    }

    private List<String> getStandardSectionsForWorkflow(String choice) {
        return switch (choice) {
            case "2" -> List.of("Research Question", "Literature Context", "Data Collection", "Evidence Analysis", "Findings & Implications", "Conclusion");
            case "3" -> List.of("Executive Summary", "Problem Statement", "Current Observations", "Technical Analysis", "Recommendations", "Next Steps");
            case "4" -> List.of("Title & Hook", "Introduction", "Core Concepts", "Practical Examples & Code", "Key Takeaways");
            case "5" -> List.of("System Overview", "Architecture & Modules", "Installation & Setup", "Configuration", "API Reference", "Troubleshooting & FAQ");
            case "6" -> List.of("Executive Summary", "Market Problem", "Proposed Solution", "Implementation Roadmap", "Budget & ROI Forecast", "Conclusion & Call to Action");
            case "7" -> List.of("Analysis Objective", "Data Ingestion & Scope", "Statistical Metrics", "Pattern Detection", "Actionable Insights");
            case "8" -> List.of("Project Overview", "Core Requirements", "Implementation Plan", "Verification");
            default -> List.of("Abstract", "Introduction", "Literature Review", "Research Gap", "Methodology", "Data Analysis", "Results and Discussion", "Conclusion", "References");
        };
    }

    private String getDefaultPriorityForSection(String section) {
        String lower = section.toLowerCase();
        if (lower.contains("literature") || lower.contains("gap") || lower.contains("methodology") ||
            lower.contains("research") || lower.contains("question") || lower.contains("problem") ||
            lower.contains("architecture") || lower.contains("objective") || lower.contains("executive")) {
            return "HIGH";
        } else if (lower.contains("references") || lower.contains("faq") || lower.contains("takeaways") || lower.contains("next steps")) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private String getRecommendedAgentForSection(String section) {
        String lower = section.toLowerCase();
        if (lower.contains("literature") || lower.contains("research") || lower.contains("reference") || lower.contains("background") || lower.contains("context")) {
            return "Research Specialist";
        } else if (lower.contains("gap") || lower.contains("analysis") || lower.contains("problem") || lower.contains("architecture") || lower.contains("evidence")) {
            return "Analysis Specialist";
        } else if (lower.contains("data") || lower.contains("metric") || lower.contains("statistic") || lower.contains("budget") || lower.contains("code")) {
            return "Data & Metrics Analyst";
        } else if (lower.contains("review") || lower.contains("audit") || lower.contains("verification")) {
            return "Quality Reviewer";
        } else {
            return "Technical Writer";
        }
    }
}
