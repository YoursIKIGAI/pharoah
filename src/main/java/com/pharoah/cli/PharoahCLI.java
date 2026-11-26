package com.pharoah.cli;

import com.pharoah.agent.AgentConfig;
import com.pharoah.agent.AgentRole;
import com.pharoah.agent.AgentService;
import com.pharoah.analytics.TaskAnalyticsEngine;
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
import com.pharoah.orchestrator.*;
import com.pharoah.persistence.dao.UserActivityDAO;
import com.pharoah.project.Project;
import com.pharoah.project.ProjectService;
import com.pharoah.tasks.TaskService;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;

// VARIJ: Central Interactive Command-Line Interface for PHAROAH.
// Provides user-defined agent deployment, DAG dependency configuration,
// live concurrent execution dashboard, immediate agent output inspection, and MySQL persistence inspection.
public class PharoahCLI {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final AgentService agentService;
    private final com.pharoah.orchestrator.TaskOrchestrator legacyOrchestrator;
    private final KnowledgeService knowledgeService;
    private final DocumentWorkflowService documentService;
    private final TaskAnalyticsEngine analyticsEngine;
    private final UserActivityDAO userActivityDAO;
    private final WorkflowOrchestrator workflowOrchestrator;
    private final Scanner scanner;

    public PharoahCLI(ProjectService projectService,
                      TaskService taskService,
                      AgentService agentService,
                      com.pharoah.orchestrator.TaskOrchestrator legacyOrchestrator,
                      KnowledgeService knowledgeService,
                      DocumentWorkflowService documentService,
                      TaskAnalyticsEngine analyticsEngine,
                      UserActivityDAO userActivityDAO) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.agentService = agentService;
        this.legacyOrchestrator = legacyOrchestrator;
        this.knowledgeService = knowledgeService;
        this.documentService = documentService;
        this.analyticsEngine = analyticsEngine;
        this.userActivityDAO = userActivityDAO;
        this.workflowOrchestrator = new WorkflowOrchestrator(userActivityDAO);
        this.scanner = new Scanner(System.in);
    }

    public void startShell() {
        boolean running = true;
        while (running) {
            TerminalUI.printBanner("2.0-LTS", "MySQL 8.0", userActivityDAO != null);

            TerminalUI.printSectionHeader("SELECT WORKFLOW CATEGORY");
            TerminalUI.printMenuOption("1", "Research Paper", "Academic format with literature review & gap analysis");
            TerminalUI.printMenuOption("2", "Creative Writing", "World-building, character concept, plot, and dialogue");
            TerminalUI.printMenuOption("3", "Technical Documentation", "Architecture, code patterns, and API reference");
            TerminalUI.printMenuOption("4", "Business Report", "Market analysis, financial ROI, and risk governance");
            TerminalUI.printMenuOption("5", "Custom Workflow", "User-defined agents, custom priorities & dependencies");
            TerminalUI.printDivider();
            TerminalUI.printMenuOption("9", "Audit Log (MySQL)", "View persistent activity history from MySQL database");
            TerminalUI.printMenuOption("10", "Advanced Tools", "Project workspaces, task graphs, knowledge repository");
            TerminalUI.printMenuOption("0", "Exit PHAROAH", "Clean shutdown and thread pool release");
            TerminalUI.printSectionFooter();

            System.out.print("  Select an option (0-10): ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleWorkflowCategory(WorkflowCategory.RESEARCH_PAPER);
                    case "2" -> handleWorkflowCategory(WorkflowCategory.CREATIVE_WRITING);
                    case "3" -> handleWorkflowCategory(WorkflowCategory.TECHNICAL_DOCUMENTATION);
                    case "4" -> handleWorkflowCategory(WorkflowCategory.BUSINESS_REPORT);
                    case "5" -> handleWorkflowCategory(WorkflowCategory.CUSTOM_WORKFLOW);
                    case "9" -> handleViewActivityLog();
                    case "10" -> handleAdvancedMenu();
                    case "0", "exit", "quit" -> {
                        TerminalUI.printSuccess("Shutting down cleanly. All persistent data preserved in MySQL.");
                        running = false;
                    }
                    default -> TerminalUI.printWarning("Invalid option. Please choose between 0 and 10.");
                }
            } catch (Exception e) {
                TerminalUI.printError("Execution error: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // USER-DEFINED AGENT DEPLOYMENT & CONCURRENT ORCHESTRATION
    // =========================================================================

    private void handleWorkflowCategory(WorkflowCategory category) {
        TerminalUI.printBreadcrumb(category.getDisplayName(), "Topic Scoping");
        System.out.println("  Enter your target inquiry, topic, or objective:");
        System.out.print("  > ");
        String topic = scanner.nextLine().trim();
        if (topic.isEmpty()) {
            topic = "Impact of Artificial Intelligence on Cybersecurity";
            TerminalUI.printInfo("Using default topic: " + topic);
        }

        // 1. Propose category-specific deployment plan
        List<AgentDeployment> agents = category.createProposedDeployment(topic);
        int maxWorkers = 4;

        boolean configuring = true;
        while (configuring) {
            TerminalUI.printBreadcrumb(category.getDisplayName(), "Deployment Configuration");
            displayDeploymentPlan(category, topic, agents, maxWorkers);

            System.out.println("  Workflow Deployment Options:");
            System.out.println("    [1] Accept proposed plan & Launch Swarm (Press Enter to Start)");
            System.out.println("    [2] Customize agent priorities");
            System.out.println("    [3] Modify agent dependencies");
            System.out.println("    [4] Set maximum concurrent worker threads");
            System.out.println("    [5] Add a new specialized agent");
            System.out.println("    [6] Remove an agent");
            System.out.println("    [0] Cancel and return to main menu");
            System.out.print("\n  Enter choice (0-6) [default 1 to launch swarm]: ");
            String opt = scanner.nextLine().trim();

            switch (opt) {
                case "", "1" -> {
                    configuring = false;
                    launchWorkflow(category, topic, agents, maxWorkers);
                }
                case "2" -> customizePriorities(agents);
                case "3" -> modifyDependencies(agents);
                case "4" -> maxWorkers = configureWorkerThreads(maxWorkers);
                case "5" -> addNewAgent(agents);
                case "6" -> removeAgent(agents);
                case "0" -> {
                    TerminalUI.printWarning("Workflow cancelled.");
                    return;
                }
                default -> TerminalUI.printWarning("Invalid choice.");
            }
        }
    }

    private void displayDeploymentPlan(WorkflowCategory category, String topic, List<AgentDeployment> agents, int maxWorkers) {
        TerminalUI.printSectionHeader(String.format("%s DEPLOYMENT PLAN (%d AGENTS, %d WORKER THREADS)",
                category.getDisplayName().toUpperCase(), agents.size(), maxWorkers));

        System.out.printf("  %s %-8s │ %-26s │ %-8s │ %s%s\n",
                TerminalUI.BOLD, "ID", "Agent Responsibility", "Priority", "Dependencies", TerminalUI.RESET);
        TerminalUI.printDivider();

        for (AgentDeployment a : agents) {
            String depStr = a.isIndependent() ? TerminalUI.GREEN + "Independent" + TerminalUI.RESET : TerminalUI.YELLOW + String.join(", ", a.getDependencies()) + TerminalUI.RESET;
            System.out.printf("  %-8s │ %-26s │ %-8s │ %s\n",
                    a.getId(),
                    a.getResponsibility().length() > 26 ? a.getResponsibility().substring(0, 23) + "..." : a.getResponsibility(),
                    a.getPriority(),
                    depStr);
        }
        TerminalUI.printSectionFooter();
    }

    private void customizePriorities(List<AgentDeployment> agents) {
        TerminalUI.printBreadcrumb("Configure Priorities");
        for (AgentDeployment a : agents) {
            System.out.printf("  Priority for '%s' ([H]igh, [M]edium, [L]ow) [current: %s]: ", a.getName(), a.getPriority());
            String in = scanner.nextLine().trim().toUpperCase();
            if (in.startsWith("H")) a.setPriority(Priority.HIGH);
            else if (in.startsWith("L")) a.setPriority(Priority.LOW);
            else if (in.startsWith("M")) a.setPriority(Priority.MEDIUM);
        }
        TerminalUI.printSuccess("Priorities updated.");
    }

    private void modifyDependencies(List<AgentDeployment> agents) {
        TerminalUI.printBreadcrumb("Configure Dependencies");
        System.out.print("  Enter Agent ID to edit dependencies (e.g. agent-03): ");
        String id = scanner.nextLine().trim();
        AgentDeployment target = null;
        for (AgentDeployment a : agents) {
            if (a.getId().equalsIgnoreCase(id)) {
                target = a;
                break;
            }
        }
        if (target == null) {
            TerminalUI.printWarning("Agent not found.");
            return;
        }

        System.out.printf("  Current dependencies for %s: %s\n", target.getId(), target.getDependencies());
        System.out.println("  Enter new comma-separated dependency IDs (or 'NONE' for independent):");
        System.out.print("  > ");
        String depIn = scanner.nextLine().trim();

        if (depIn.equalsIgnoreCase("NONE") || depIn.isEmpty()) {
            target.clearDependencies();
            TerminalUI.printSuccess(target.getId() + " is now independent.");
        } else {
            target.clearDependencies();
            for (String dep : depIn.split(",")) {
                target.addDependency(dep.trim());
            }
            TerminalUI.printSuccess("Dependencies updated for " + target.getId());
        }
    }

    private int configureWorkerThreads(int current) {
        System.out.printf("  Enter max concurrent worker threads (1-16) [current: %d]: ", current);
        try {
            int val = Integer.parseInt(scanner.nextLine().trim());
            if (val >= 1 && val <= 16) {
                TerminalUI.printSuccess("Max concurrent worker threads set to: " + val);
                return val;
            }
        } catch (Exception ignored) {}
        return current;
    }

    private void addNewAgent(List<AgentDeployment> agents) {
        String newId = String.format("agent-%02d", agents.size() + 1);
        System.out.print("  Agent Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("  Responsibility description: ");
        String resp = scanner.nextLine().trim();
        System.out.print("  Priority ([H]igh, [M]edium, [L]ow): ");
        String pIn = scanner.nextLine().trim().toUpperCase();
        Priority prio = pIn.startsWith("H") ? Priority.HIGH : (pIn.startsWith("L") ? Priority.LOW : Priority.MEDIUM);
        System.out.print("  Dependencies (comma-separated IDs or NONE): ");
        String depsIn = scanner.nextLine().trim();
        List<String> deps = new ArrayList<>();
        if (!depsIn.equalsIgnoreCase("NONE") && !depsIn.isBlank()) {
            for (String d : depsIn.split(",")) deps.add(d.trim());
        }

        AgentDeployment created = new AgentDeployment(newId, name, "CUSTOM_SPECIALIST", resp, prio, deps);
        agents.add(created);
        TerminalUI.printSuccess("Added new agent: " + created.getId() + " - " + created.getName());
    }

    private void removeAgent(List<AgentDeployment> agents) {
        if (agents.size() <= 1) {
            TerminalUI.printWarning("Cannot remove: At least one agent is required.");
            return;
        }
        System.out.print("  Enter Agent ID to remove: ");
        String id = scanner.nextLine().trim();
        agents.removeIf(a -> a.getId().equalsIgnoreCase(id));
        TerminalUI.printSuccess("Agent removed if ID existed.");
    }

    private void launchWorkflow(WorkflowCategory category, String topic, List<AgentDeployment> agents, int maxWorkers) {
        TerminalUI.printBreadcrumb(category.getDisplayName(), "Concurrent Orchestration");

        try {
            WorkflowPlan plan = new WorkflowPlan(category, topic, agents, maxWorkers);
            TerminalUI.printInfo("Validated DAG dependency graph. Initiating Java multithreading swarm...\n");

            WorkflowExecutionResult result = workflowOrchestrator.executeWorkflow(plan);

            if (result.isSuccess()) {
                TerminalUI.printSuccess("Multi-Agent Swarm execution completed successfully!");
                TerminalUI.printInfo(String.format("Total Latency: %.2f seconds | Deployed Swarm: %d Agents | Worker Pool: %d Threads",
                        result.getDurationSeconds(), result.getAgentsUsed().size(), maxWorkers));
                TerminalUI.printInfo("Persisted Document Artifact: " + result.getOutputFilePath());

                // Interactive Agent Output & Artifact Viewer
                handlePostExecutionViewer(result, plan);
            } else {
                TerminalUI.printError("Workflow encountered errors during execution.");
                System.out.print("\n  Press Enter to return to main menu...");
                scanner.nextLine();
            }

        } catch (Exception e) {
            TerminalUI.printError("Orchestration failed: " + e.getMessage());
            System.out.print("\n  Press Enter to return to main menu...");
            scanner.nextLine();
        }
    }

    // =========================================================================
    // POST-EXECUTION ARTIFACT VIEWER & AGENT OUTPUT INSPECTION
    // =========================================================================

    private void handlePostExecutionViewer(WorkflowExecutionResult result, WorkflowPlan plan) {
        boolean viewing = true;
        Map<String, AgentOutput> outputs = result.getAgentOutputs();

        while (viewing) {
            TerminalUI.printSectionHeader(String.format("SWARM AGENT OUTPUTS & ARTIFACTS: %s", result.getTopic().toUpperCase()));

            System.out.printf("  %s %-10s │ %-28s │ %-10s │ %-8s │ %s%s\n",
                    TerminalUI.BOLD, "AGENT ID", "AGENT NAME", "DURATION", "WORDS", "THREAD", TerminalUI.RESET);
            TerminalUI.printDivider();

            for (AgentDeployment a : plan.getAgents()) {
                AgentOutput out = outputs.get(a.getId());
                long dur = out != null ? out.getDurationMs() : a.getExecutionDurationMs();
                int words = out != null ? out.getWordCount() : 0;
                String thread = out != null ? out.getThreadName() : a.getAssignedThreadName();

                System.out.printf("  %-10s │ %-28s │ %6d ms  │ %4d wds │ %s\n",
                        a.getId(),
                        a.getName().length() > 28 ? a.getName().substring(0, 25) + "..." : a.getName(),
                        dur,
                        words,
                        thread != null ? thread : "Worker");
            }
            TerminalUI.printSectionFooter();

            System.out.println("  Swarm Artifact Actions:");
            System.out.println("    [1] 📄 View Full Compiled Document in Terminal (Read entire output)");
            System.out.println("    [2] 🤖 Inspect Specific Agent Output (Literature, Methodology, Data, etc.)");
            System.out.println("    [3] 📂 View Output File Path & Open Instructions");
            System.out.println("    [0] 🔙 Return to Main Menu");
            System.out.print("\n  Select an action (0-3) [default 1 to read document]: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "", "1" -> displayFullCompiledDocument(result);
                case "2" -> inspectSpecificAgentOutput(plan, outputs);
                case "3" -> displayOutputFileLocation(result);
                case "0", "exit", "back" -> viewing = false;
                default -> TerminalUI.printWarning("Invalid choice. Please enter 0-3.");
            }
        }
    }

    private void displayFullCompiledDocument(WorkflowExecutionResult result) {
        TerminalUI.printBreadcrumb("Swarm Output", "Consolidated Document Artifact");
        TerminalUI.printSectionHeader("CONSOLIDATED DOCUMENT: " + result.getTopic().toUpperCase());
        System.out.println();
        System.out.println(result.getFullDocument());
        System.out.println();
        TerminalUI.printSectionFooter();
        System.out.print("\n  Press Enter to return to artifact actions...");
        scanner.nextLine();
    }

    private void inspectSpecificAgentOutput(WorkflowPlan plan, Map<String, AgentOutput> outputs) {
        TerminalUI.printBreadcrumb("Swarm Output", "Agent Inspector");
        List<AgentDeployment> agents = plan.getAgents();

        System.out.println("\n  Available Agents to Inspect:");
        for (int i = 0; i < agents.size(); i++) {
            AgentDeployment a = agents.get(i);
            AgentOutput out = outputs.get(a.getId());
            int words = out != null ? out.getWordCount() : 0;
            System.out.printf("    [%d] %-8s : %-28s (%d words, Priority: %s)\n",
                    i + 1, a.getId(), a.getName(), words, a.getPriority());
        }
        System.out.println("    [0] Back to Artifact Actions");
        System.out.print("\n  Select agent number (1-" + agents.size() + "): ");

        String in = scanner.nextLine().trim();
        try {
            int idx = Integer.parseInt(in) - 1;
            if (idx >= 0 && idx < agents.size()) {
                AgentDeployment selected = agents.get(idx);
                AgentOutput out = outputs.get(selected.getId());

                TerminalUI.printSectionHeader(String.format("OUTPUT: %s (%s)", selected.getName().toUpperCase(), selected.getId()));
                System.out.printf("  Responsibility : %s\n", selected.getResponsibility());
                System.out.printf("  Assigned Thread: %s | Execution Time: %d ms | Words: %d\n",
                        out != null ? out.getThreadName() : selected.getAssignedThreadName(),
                        out != null ? out.getDurationMs() : selected.getExecutionDurationMs(),
                        out != null ? out.getWordCount() : 0);
                if (out != null && !out.getCitations().isEmpty()) {
                    System.out.println("  Citations Cited: " + String.join("; ", out.getCitations()));
                }
                TerminalUI.printDivider();
                System.out.println();
                if (out != null && !out.getContent().isBlank()) {
                    System.out.println(out.getContent());
                } else {
                    System.out.println("  [!] No content was recorded for this agent.");
                }
                System.out.println();
                TerminalUI.printSectionFooter();

                System.out.print("\n  Press Enter to continue...");
                scanner.nextLine();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void displayOutputFileLocation(WorkflowExecutionResult result) {
        TerminalUI.printBreadcrumb("Swarm Output", "File Details");
        File file = new File(result.getOutputFilePath());
        TerminalUI.printSectionHeader("OUTPUT FILE DETAILS");
        System.out.printf("  Topic          : %s\n", result.getTopic());
        System.out.printf("  Category       : %s\n", result.getCategory());
        System.out.printf("  Relative Path  : %s\n", result.getOutputFilePath());
        System.out.printf("  Absolute Path  : %s\n", file.getAbsolutePath());
        System.out.printf("  File Exists    : %b\n", file.exists());
        System.out.printf("  File Size      : %d bytes\n", file.length());
        System.out.println("\n  To view or edit in VS Code:");
        System.out.printf("    code \"%s\"\n", file.getAbsolutePath());
        TerminalUI.printSectionFooter();
        System.out.print("\n  Press Enter to return to artifact actions...");
        scanner.nextLine();
    }

    // =========================================================================
    // MYSQL ACTIVITY LOG AUDIT
    // =========================================================================

    private void handleViewActivityLog() {
        TerminalUI.printBreadcrumb("MySQL Activity History");

        if (userActivityDAO == null) {
            TerminalUI.printWarning("Database persistence is offline.");
            return;
        }

        List<UserActivity> activities = userActivityDAO.getAllActivities();
        if (activities.isEmpty()) {
            TerminalUI.printInfo("No previous activities recorded in MySQL yet.");
        } else {
            TerminalUI.printSectionHeader(String.format("PERSISTENT USER ACTIVITY LOG (%d RECORDS)", activities.size()));
            for (UserActivity a : activities) {
                System.out.printf("  #%-3d │ %-22s │ %-28s │ %s\n",
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
    // ADVANCED TOOLS SUB-MENU
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
                case "4" -> handleLegacyDecomposition();
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
                TerminalUI.printSuccess("Project created: " + p.getName());
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
        System.out.println("  5. Execute All Tasks in Current Project Swarm");
        System.out.print("  Choose (1-5): ");
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
            case "5" -> {
                List<Task> tasks = taskService.getTasksByProject(pid);
                TerminalUI.printInfo(String.format("Executing %d tasks in project '%s' across worker pool...", tasks.size(), pid));
                for (Task t : tasks) {
                    if (t.getStatus() != TaskStatus.COMPLETED) {
                        try {
                            Future<TaskResult> future = legacyOrchestrator.submitTaskAsync(t);
                            TaskResult res = future.get();
                            TerminalUI.printSuccess(String.format("[%s] Done by '%s' (%d ms)", t.getTitle(), res.executedByAgent(), res.executionDurationMs()));
                            if (t.getFinalOutput() != null && !t.getFinalOutput().isBlank()) {
                                System.out.println("     Output: " + (t.getFinalOutput().length() > 140 ? t.getFinalOutput().substring(0, 137) + "..." : t.getFinalOutput()));
                            }
                        } catch (Exception e) {
                            TerminalUI.printError("Execution error on task " + t.getTitle() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void handleAgentMenu() throws SQLException {
        TerminalUI.printBreadcrumb("Advanced Modules", "Agent Registry");
        List<AgentConfig> agents = agentService.listAllAgents();
        for (AgentConfig a : agents) {
            System.out.printf("   • %-24s | Role: %-20s | Active: %b\n", a.getName(), a.getRole(), a.isActive());
        }
    }

    private void handleLegacyDecomposition() throws Exception {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        TerminalUI.printBreadcrumb("Advanced Modules", "Task Decomposition");
        System.out.print("  Enter high-level objective: ");
        String title = scanner.nextLine().trim();
        System.out.print("  Enter domain topic: ");
        String topic = scanner.nextLine().trim();

        List<Task> subtasks = legacyOrchestrator.decomposeObjective(pid, title, topic);
        TerminalUI.printSuccess(String.format("Decomposed into %d subtasks with dependency graph.", subtasks.size()));

        System.out.print("\n  Deploy and execute these subtasks across worker threads now? (Y/n) [default Y]: ");
        String execChoice = scanner.nextLine().trim();
        if (execChoice.isEmpty() || execChoice.equalsIgnoreCase("y") || execChoice.equalsIgnoreCase("yes")) {
            TerminalUI.printInfo("Dispatching subtasks to ThreadPoolManager...");
            for (Task t : subtasks) {
                Future<TaskResult> future = legacyOrchestrator.submitTaskAsync(t);
                try {
                    TaskResult res = future.get();
                    TerminalUI.printSuccess(String.format("[%s] Completed by '%s' (%d ms)", t.getTitle(), res.executedByAgent(), res.executionDurationMs()));
                    System.out.println("     Output Preview: " + (t.getFinalOutput() != null && t.getFinalOutput().length() > 140 ? t.getFinalOutput().substring(0, 137) + "..." : t.getFinalOutput()));
                } catch (Exception e) {
                    TerminalUI.printError("Task execution failed: " + e.getMessage());
                }
            }
        }
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
}
