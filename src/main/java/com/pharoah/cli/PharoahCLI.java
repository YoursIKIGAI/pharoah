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

// VARIJ & SANCHIT: Implemented PharoahCLI.
// The primary terminal-based user interface connecting format-driven workflows,
// the Python Multi-Agent Swarm, MySQL activity logging, and project modules.
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

    // VARIJ: Primary interactive terminal loop implementing Format-Driven Multi-Agent Workflow
    public void startShell() {
        printBanner();

        boolean running = true;
        while (running) {
            System.out.println("========================================");
            System.out.println("              PHAROAH");
            System.out.println("       MULTI-AGENT WORKSPACE");
            System.out.println("========================================");
            System.out.println();
            System.out.println("What would you like to work on?");
            System.out.println();
            System.out.println("1. Research Paper");
            System.out.println("2. Research Analysis");
            System.out.println("3. Report Generation");
            System.out.println("4. Blog Writing");
            System.out.println("5. Technical Documentation");
            System.out.println("6. Business Proposal");
            System.out.println("7. Data Analysis");
            System.out.println("8. Custom Workflow");
            System.out.println("----------------------------------------");
            System.out.println("9. View MySQL Activity Audit Log");
            System.out.println("10. Advanced Modules (Projects, Tasks, Knowledge)");
            System.out.println("0. Exit PHAROAH");
            System.out.println("========================================");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1", "2", "3", "4", "5", "6", "7", "8" -> handleFormatDrivenWorkflow(choice);
                    case "9" -> handleViewActivityLog();
                    case "10" -> handleAdvancedMenu();
                    case "0", "exit", "quit" -> {
                        System.out.println("\n[PHAROAH] Shutting down cleanly. All persistent data preserved in MySQL.");
                        running = false;
                    }
                    default -> System.out.println("Invalid selection. Please choose an option from the menu.");
                }
            } catch (Exception e) {
                System.err.println("\n[ERROR] An exception occurred during execution: " + e.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("""
            =========================================================================
               👑 PHAROAH: Format-Driven Multi-Agent Task Intelligence System
               Hybrid Architecture: Java Terminal UI + Python Multi-Agent Swarm
               Database: MySQL 8.0 (JDBC) | Real-Time Execution Streaming
               Team: Varij (Orchestration & CLI) | Ashutosh (Agents & Swarm) | Sanchit (Persistence & MySQL)
            =========================================================================
            """);
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

        // STEP 1: Workflow already confirmed
        System.out.println("\n========================================");
        System.out.println("  FORMAT SELECTED: " + workflowName.toUpperCase());
        System.out.println("========================================");

        // STEP 2: SELECT THE TOPIC
        System.out.println("\nEnter your topic (or press Enter for default):");
        System.out.print("> ");
        String topic = scanner.nextLine().trim();
        if (topic.isEmpty()) {
            topic = "Impact of Artificial Intelligence on Cybersecurity";
            System.out.println("Using default topic: " + topic);
        }

        // STEP 3: DISPLAY THE DOCUMENT FORMAT & SELECT SECTIONS
        List<String> availableSections = getStandardSectionsForWorkflow(choice);
        if ("8".equals(choice)) {
            System.out.println("\nEnter custom section names separated by commas (e.g., Problem, Solution, Verification):");
            System.out.print("> ");
            String customInput = scanner.nextLine().trim();
            if (!customInput.isEmpty()) {
                availableSections = Arrays.stream(customInput.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        }

        System.out.println("\n----------------------------------------");
        System.out.println(" " + workflowName.toUpperCase() + " WORKFLOW");
        System.out.println("----------------------------------------");
        for (int i = 0; i < availableSections.size(); i++) {
            System.out.printf(" %d. %s\n", i + 1, availableSections.get(i));
        }
        System.out.println("----------------------------------------");
        System.out.println("What would you like to work on?");
        System.out.println("  [A] All Sections (Generate Complete Document)");
        System.out.println("  Or enter section numbers separated by commas (e.g., 3, 4, 5):");
        System.out.print("> ");
        String secInput = scanner.nextLine().trim();

        List<String> selectedSections = new ArrayList<>();
        if (secInput.equalsIgnoreCase("A") || secInput.isEmpty()) {
            selectedSections.addAll(availableSections);
        } else {
            String[] tokens = secInput.split(",");
            for (String tok : tokens) {
                tok = tok.trim();
                try {
                    int secIdx = Integer.parseInt(tok) - 1;
                    if (secIdx >= 0 && secIdx < availableSections.size()) {
                        selectedSections.add(availableSections.get(secIdx));
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (selectedSections.isEmpty()) {
                selectedSections.addAll(availableSections);
            }
        }

        System.out.println("\nSELECTED SECTIONS:");
        for (String s : selectedSections) {
            System.out.println("  - " + s);
        }

        // STEP 4: PRIORITY SELECTION
        Map<String, String> priorities = new LinkedHashMap<>();
        System.out.println("\n----------------------------------------");
        System.out.println("ASSIGN PRIORITIES:");
        System.out.println("----------------------------------------");
        for (String s : selectedSections) {
            String defPrio = getDefaultPriorityForSection(s);
            priorities.put(s, defPrio);
            System.out.printf("  %-26s : %s\n", s, defPrio);
        }

        System.out.println("\n1. Accept priorities");
        System.out.println("2. Customize priorities (HIGH / MEDIUM / LOW)");
        System.out.print("Enter choice [default 1]: ");
        String prioChoice = scanner.nextLine().trim();
        if ("2".equals(prioChoice)) {
            for (String s : selectedSections) {
                System.out.printf("  Priority for '%s' ([H]igh, [M]edium, [L]ow) [default %s]: ", s, priorities.get(s));
                String pIn = scanner.nextLine().trim().toUpperCase();
                if (pIn.startsWith("H")) priorities.put(s, "HIGH");
                else if (pIn.startsWith("L")) priorities.put(s, "LOW");
                else if (pIn.startsWith("M")) priorities.put(s, "MEDIUM");
            }
        }

        // STEP 5: AGENT SELECTION AND ASSIGNMENT
        Map<String, String> agentAssignments = new LinkedHashMap<>();
        System.out.println("\n----------------------------------------");
        System.out.println("AGENT ASSIGNMENT");
        System.out.println("----------------------------------------");
        for (String s : selectedSections) {
            String recAgent = getRecommendedAgentForSection(s);
            agentAssignments.put(s, recAgent);
            System.out.println("  " + s);
            System.out.println("      " + recAgent);
        }
        System.out.println("  Final Output");
        System.out.println("      Reviewer Agent\n");

        System.out.println("1. Accept assignments & start workflow");
        System.out.println("2. Customize agents");
        System.out.println("3. Cancel workflow");
        System.out.print("Enter choice [default 1]: ");
        String agentChoice = scanner.nextLine().trim();
        if ("3".equals(agentChoice)) {
            System.out.println("Workflow cancelled.");
            return;
        } else if ("2".equals(agentChoice)) {
            System.out.println("Available Agent Roles: Research Specialist, Analysis Specialist, Technical Writer, Data & Metrics Analyst");
            for (String s : selectedSections) {
                System.out.printf("  Agent for '%s' [default: %s]: ", s, agentAssignments.get(s));
                String aIn = scanner.nextLine().trim();
                if (!aIn.isEmpty()) {
                    agentAssignments.put(s, aIn);
                }
            }
        }

        // EXECUTION: Stream live via PythonAgentBridge
        System.out.println("\n=========================================================================");
        System.out.println("  🚀 STARTING MULTI-AGENT WORKFLOW: " + workflowName.toUpperCase());
        System.out.println("=========================================================================\n");

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

        // PERSISTENCE: Save activity in MySQL user_activity table
        System.out.println("\n=========================================================================");
        System.out.println("  💾 DATABASE PERSISTENCE (SANCHIT: MySQL JDBC)");
        System.out.println("=========================================================================");

        String sectionsWorked = String.join(", ", selectedSections);
        String agentsUsed = (result.getAgentsUsed() != null && !result.getAgentsUsed().isEmpty())
                ? String.join(", ", result.getAgentsUsed())
                : String.join(", ", new HashSet<>(agentAssignments.values()));

        if (userActivityDAO != null) {
            UserActivity activity = new UserActivity(
                    "Student",
                    topic,
                    workflowName,
                    sectionsWorked,
                    agentsUsed
            );
            userActivityDAO.recordActivity(activity);
            System.out.println("  [SUCCESS] Persistent record written to MySQL 'user_activity' table.");
            System.out.printf("  Record Details  : User=Student | Workflow=%s | Topic=%s\n", workflowName, topic);
            System.out.printf("  Sections Worked : %s\n", sectionsWorked);
            System.out.printf("  Agents Deployed : %s\n", agentsUsed);
            System.out.println("  MySQL Workbench : Query 'SELECT * FROM pharoah_db.user_activity;' to verify.");
        } else {
            System.out.println("  [NOTE] Activity recorded in local session context.");
        }
        System.out.println("=========================================================================\n");
    }

    // SANCHIT: Displays user activity audit trail from MySQL Workbench table
    private void handleViewActivityLog() {
        System.out.println("\n=========================================================================");
        System.out.println("         📜 PERSISTENT USER ACTIVITY LOG (MySQL Workbench View)         ");
        System.out.println("=========================================================================");

        if (userActivityDAO == null) {
            System.out.println("  Database persistence DAO is not initialized.");
            return;
        }

        List<UserActivity> activities = userActivityDAO.getAllActivities();
        if (activities.isEmpty()) {
            System.out.println("  No activities recorded yet in 'user_activity' table.");
            System.out.println("  Run any workflow (Options 1-8) to record execution history.");
        } else {
            System.out.printf("  Total Recorded Activities: %d\n\n", activities.size());
            for (UserActivity a : activities) {
                System.out.printf("  [ID: %2d] Topic    : %s\n", a.getActivityId(), a.getTopic());
                System.out.printf("           Workflow : %s | Status: %s | Time: %s\n", a.getWorkflowType(), a.getStatus(), a.getCreatedAt());
                System.out.printf("           Sections : %s\n", a.getSectionsWorked());
                System.out.printf("           Agents   : %s\n", a.getAgentsUsed());
                System.out.println("  " + "-".repeat(70));
            }
        }
        System.out.println("=========================================================================\n");
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

    // =========================================================================
    // ADVANCED MODULES SUB-MENU (PROJECTS, TASKS, KNOWLEDGE, ANALYTICS)
    // =========================================================================

    private void handleAdvancedMenu() throws Exception {
        boolean inSub = true;
        while (inSub) {
            Project active = projectService.getActiveProject();
            String activeName = (active != null) ? active.getName() : "None";

            System.out.println("\n-------------------------------------------------------------------------");
            System.out.printf("  ⚙️ ADVANCED SYSTEM MODULES | Active Project: [%s]\n", activeName);
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("  [1] 📁 Project Management     - Create, list, switch, and delete projects");
            System.out.println("  [2] 📋 Task Management        - Manage individual tasks, status, and human review");
            System.out.println("  [3] 👥 Agent Registry         - Register and configure agent profiles");
            System.out.println("  [4] ⚡ Central Orchestration   - Decompose objectives into task graphs");
            System.out.println("  [5] 📚 Knowledge Repository   - Store URLs, code snippets, notes");
            System.out.println("  [6] 📊 Stream API Analytics   - GroupingBy, PartitioningBy, Payload stats");
            System.out.println("  [7] 🔙 Return to Main Menu");
            System.out.println("-------------------------------------------------------------------------");
            System.out.print("Select an option (1-7): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> handleProjectMenu();
                case "2" -> handleTaskMenu();
                case "3" -> handleAgentMenu();
                case "4" -> handleOrchestrationMenu();
                case "5" -> handleKnowledgeMenu();
                case "6" -> handleAnalyticsMenu();
                case "7", "back", "0" -> inSub = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    // ================= 1. PROJECT MANAGEMENT =================
    private void handleProjectMenu() throws SQLException {
        System.out.println("\n--- [MODULE A] PROJECT MANAGEMENT ---");
        System.out.println("1. List All Projects");
        System.out.println("2. Create New Project");
        System.out.println("3. Switch Active Project");
        System.out.println("4. Delete Project");
        System.out.print("Choose (1-4): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                List<Project> list = projectService.listAllProjects();
                System.out.printf("\nTotal Projects (%d):\n", list.size());
                for (Project p : list) {
                    boolean isCurr = projectService.getActiveProject() != null && projectService.getActiveProject().getProjectId().equals(p.getProjectId());
                    System.out.printf("  %s %s\n", isCurr ? "➔ [ACTIVE]" : "  [      ]", p);
                }
            }
            case "2" -> {
                System.out.print("Enter Project Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Enter Description: ");
                String desc = scanner.nextLine().trim();
                Project p = projectService.createProject(name, desc);
                System.out.println("Successfully created and activated project: " + p.getName());
            }
            case "3" -> {
                List<Project> list = projectService.listAllProjects();
                for (int i = 0; i < list.size(); i++) {
                    System.out.printf("  [%d] %s (%s)\n", i + 1, list.get(i).getName(), list.get(i).getProjectId().substring(0, Math.min(8, list.get(i).getProjectId().length())));
                }
                System.out.print("Select project number: ");
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx >= 0 && idx < list.size()) {
                    projectService.setActiveProject(list.get(idx).getProjectId());
                    System.out.println("Switched active project to: " + list.get(idx).getName());
                }
            }
            case "4" -> {
                System.out.print("Enter Project ID to delete: ");
                String pid = scanner.nextLine().trim();
                if (projectService.deleteProject(pid)) {
                    System.out.println("Project deleted successfully.");
                } else {
                    System.out.println("Project not found.");
                }
            }
        }
    }

    // ================= 2. TASK MANAGEMENT =================
    private void handleTaskMenu() throws SQLException {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        System.out.println("\n--- [MODULE B] TASK MANAGEMENT (Project: " + (active != null ? active.getName() : "GLOBAL") + ") ---");
        System.out.println("1. List Tasks in Current Project");
        System.out.println("2. Create New Task");
        System.out.println("3. Assign Agent to Task");
        System.out.println("4. Review & Approve Task (Human-in-the-Loop)");
        System.out.println("5. Delete Task");
        System.out.print("Choose (1-5): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                List<Task> tasks = taskService.getTasksByProject(pid);
                System.out.printf("\nTasks in Project (%d):\n", tasks.size());
                for (Task t : tasks) {
                    System.out.println("  " + t);
                    if (t.getFinalOutput() != null && !t.getFinalOutput().isEmpty()) {
                        System.out.println("    └─ Output Preview: " + t.getFinalOutput().lines().findFirst().orElse(""));
                    }
                }
            }
            case "2" -> {
                System.out.print("Task Title: ");
                String title = scanner.nextLine().trim();
                System.out.print("Category ([1]RESEARCH [2]WRITING [3]COMPARISON [4]CODING [5]VERIFICATION): ");
                String c = scanner.nextLine().trim();
                TaskType type = switch (c) {
                    case "2" -> TaskType.WRITING;
                    case "3" -> TaskType.COMPARISON;
                    case "4" -> TaskType.CODING;
                    case "5" -> TaskType.VERIFICATION;
                    default -> TaskType.RESEARCH;
                };

                System.out.print("Priority ([H]igh, [M]edium, [L]ow): ");
                String pStr = scanner.nextLine().trim().toUpperCase();
                Priority prio = pStr.startsWith("H") ? Priority.HIGH : (pStr.startsWith("L") ? Priority.LOW : Priority.MEDIUM);

                System.out.print("Instructions / Prompt Payload: ");
                String payload = scanner.nextLine().trim();

                Task t = taskService.createTask(pid, title, type, prio, payload, null, null);
                System.out.println("Task created: " + t.getTaskId().substring(0, Math.min(8, t.getTaskId().length())) + " (Status: " + t.getStatus() + ")");
            }
            case "3" -> {
                System.out.print("Enter Task ID: ");
                String tid = scanner.nextLine().trim();
                System.out.print("Enter Agent ID: ");
                String aid = scanner.nextLine().trim();
                Task t = taskService.getTaskById(tid);
                if (t != null) {
                    t.setAssignedAgentId(aid);
                    System.out.println("Assigned agent " + aid + " to task " + tid);
                }
            }
            case "4" -> {
                System.out.print("Enter Task ID to review: ");
                String tid = scanner.nextLine().trim();
                Task t = taskService.getTaskById(tid);
                if (t == null) {
                    System.out.println("Task not found.");
                    return;
                }
                System.out.println("\n--- TASK OUTPUT REVIEW ---");
                System.out.println("Title: " + t.getTitle());
                System.out.println("Status: " + t.getStatus());
                System.out.println("Output Content:\n" + t.getFinalOutput());
                System.out.println("--------------------------");
                System.out.print("Do you APPROVE this output? (y/n): ");
                boolean approved = scanner.nextLine().trim().equalsIgnoreCase("y");
                System.out.print("Enter review notes / feedback: ");
                String notes = scanner.nextLine().trim();
                taskService.recordReviewApproval(tid, approved, notes);
                System.out.printf("Task marked as: %s\n", approved ? "COMPLETED (Approved)" : "IN_PROGRESS (Revision Requested)");
            }
            case "5" -> {
                System.out.print("Enter Task ID to delete: ");
                String tid = scanner.nextLine().trim();
                taskService.deleteTask(tid);
                System.out.println("Task deleted.");
            }
        }
    }

    // ================= 3. AGENT MANAGEMENT =================
    private void handleAgentMenu() throws SQLException {
        System.out.println("\n--- [MODULE C] AGENT SWARM MANAGEMENT ---");
        System.out.println("1. List All Registered Agents");
        System.out.println("2. Register New Configurable Agent");
        System.out.println("3. Update Agent System Instructions");
        System.out.println("4. Toggle Agent Active/Inactive");
        System.out.print("Choose (1-4): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                List<AgentConfig> agents = agentService.listAllAgents();
                System.out.printf("\nRegistered Swarm Agents (%d):\n", agents.size());
                for (AgentConfig a : agents) {
                    System.out.println("  " + a);
                }
            }
            case "2" -> {
                System.out.print("Agent Name: ");
                String name = scanner.nextLine().trim();
                System.out.println("Available Roles:");
                for (AgentRole r : AgentRole.values()) {
                    System.out.printf("  * %-20s : %s\n", r.name(), r.getDisplayName());
                }
                System.out.print("Enter Role Name: ");
                String roleStr = scanner.nextLine().trim().toUpperCase();
                AgentRole role;
                try {
                    role = AgentRole.valueOf(roleStr);
                } catch (Exception e) {
                    role = AgentRole.RESEARCH_AGENT;
                }

                System.out.print("System Instructions / Guidelines: ");
                String inst = scanner.nextLine().trim();
                System.out.print("Provider Type ([1] MOCK_HEURISTIC, [2] GEMINI_API, [3] OLLAMA): ");
                String pType = switch (scanner.nextLine().trim()) {
                    case "2" -> "GEMINI_API";
                    case "3" -> "OLLAMA";
                    default -> "MOCK_HEURISTIC";
                };

                AgentConfig created = agentService.registerAgent(name, role, inst, pType);
                System.out.println("Agent registered successfully: " + created.getName() + " (" + created.getAgentId().substring(0, Math.min(8, created.getAgentId().length())) + ")");
            }
            case "3" -> {
                System.out.print("Enter Agent ID: ");
                String aid = scanner.nextLine().trim();
                System.out.print("Enter New System Instructions: ");
                String inst = scanner.nextLine().trim();
                agentService.updateInstructions(aid, inst);
                System.out.println("Updated agent instructions.");
            }
            case "4" -> {
                System.out.print("Enter Agent ID: ");
                String aid = scanner.nextLine().trim();
                AgentConfig a = agentService.getAgentById(aid);
                if (a != null) {
                    agentService.setAgentActive(aid, !a.isActive());
                    System.out.printf("Agent %s is now: %s\n", a.getName(), !a.isActive() ? "ACTIVE" : "INACTIVE");
                }
            }
        }
    }

    // ================= 4. ORCHESTRATION =================
    private void handleOrchestrationMenu() throws Exception {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        System.out.println("\n--- [MODULE D] MULTI-AGENT ORCHESTRATION ---");
        System.out.println("1. Decompose Complex Objective into Subtasks");
        System.out.println("2. Execute a Single Task via Agent Swarm");
        System.out.println("3. Run All Pending Tasks in Parallel");
        System.out.print("Choose (1-3): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                System.out.print("Enter High-Level Objective Title: ");
                String title = scanner.nextLine().trim();
                System.out.print("Enter Domain Topic (e.g. AI in Cybersecurity): ");
                String topic = scanner.nextLine().trim();

                System.out.println("\nDecomposing objective into structured subtasks with dependency graph...");
                List<Task> subtasks = orchestrator.decomposeObjective(pid, title, topic);
                System.out.printf("Successfully generated and saved %d subtasks:\n", subtasks.size());
                for (int i = 0; i < subtasks.size(); i++) {
                    Task t = subtasks.get(i);
                    System.out.printf("  [%d] #%s | %s (Prerequisite: %s)\n",
                            i + 1, t.getTaskId().substring(0, Math.min(8, t.getTaskId().length())), t.getTitle(),
                            t.getDependsOnTaskId() != null ? t.getDependsOnTaskId().substring(0, Math.min(8, t.getDependsOnTaskId().length())) : "NONE (First)");
                }
            }
            case "2" -> {
                System.out.print("Enter Task ID to execute: ");
                String tid = scanner.nextLine().trim();
                Task t = taskService.getTaskById(tid);
                if (t == null) {
                    System.out.println("Task not found.");
                    return;
                }
                System.out.println("\nSubmitting task to concurrent thread pool...");
                Future<TaskResult> future = orchestrator.submitTaskAsync(t);
                TaskResult res = future.get();

                System.out.println("\n================= AGENT SYNTHESIZED RESULT =================");
                System.out.println(res.output());
                System.out.println("============================================================");
                System.out.println("Task status updated to: UNDER_REVIEW (Awaiting your approval).");
            }
            case "3" -> {
                List<Task> tasks = taskService.getTasksByProject(pid);
                List<Task> pending = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).toList();
                if (pending.isEmpty()) {
                    System.out.println("No pending tasks in active project.");
                    return;
                }
                System.out.printf("Executing %d pending tasks concurrently across worker threads...\n", pending.size());
                List<TaskResult> results = orchestrator.executeBatch(pending);
                System.out.printf("Batch completed: %d tasks processed.\n", results.size());
            }
        }
    }

    // ================= 5. KNOWLEDGE REPOSITORY =================
    private void handleKnowledgeMenu() throws SQLException {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        System.out.println("\n--- [MODULE E] KNOWLEDGE REPOSITORY ---");
        System.out.println("1. Save New Knowledge Asset (URL, Note, Code Snippet, Paper)");
        System.out.println("2. Search Knowledge Repository (Keyword / Topic)");
        System.out.println("3. List All Knowledge in Current Project");
        System.out.println("4. Delete Knowledge Resource");
        System.out.print("Choose (1-4): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                System.out.print("Resource Title: ");
                String title = scanner.nextLine().trim();
                System.out.print("Type ([1]URL [2]NOTE [3]CODE_SNIPPET [4]PAPER [5]REFERENCE): ");
                ResourceType type = switch (scanner.nextLine().trim()) {
                    case "2" -> ResourceType.NOTE;
                    case "3" -> ResourceType.CODE_SNIPPET;
                    case "4" -> ResourceType.PAPER;
                    case "5" -> ResourceType.REFERENCE;
                    default -> ResourceType.URL;
                };
                System.out.print("Content or URL: ");
                String content = scanner.nextLine().trim();
                System.out.print("Topic Tag (e.g. CONCURRENCY, DBMS, SECURITY): ");
                String tag = scanner.nextLine().trim().toUpperCase();

                KnowledgeResource r = knowledgeService.saveResource(pid, title, type, content, tag);
                System.out.println("Knowledge asset saved successfully (ID: " + r.getResourceId().substring(0, Math.min(8, r.getResourceId().length())) + ")");
            }
            case "2" -> {
                System.out.print("Enter search keyword (or press Enter for all): ");
                String kw = scanner.nextLine().trim();
                System.out.print("Filter by Topic Tag (or press Enter for ALL): ");
                String tag = scanner.nextLine().trim();

                List<KnowledgeResource> results = knowledgeService.searchKnowledge(kw, tag);
                System.out.printf("\nSearch Results (%d matches):\n", results.size());
                for (KnowledgeResource r : results) {
                    System.out.println("  " + r);
                }
            }
            case "3" -> {
                List<KnowledgeResource> list = knowledgeService.getResourcesByProject(pid);
                System.out.printf("\nKnowledge Assets in [%s] (%d):\n", (active != null ? active.getName() : "GLOBAL"), list.size());
                for (KnowledgeResource r : list) {
                    System.out.println("  " + r);
                }
            }
            case "4" -> {
                System.out.print("Enter Resource ID to delete: ");
                String rid = scanner.nextLine().trim();
                knowledgeService.deleteResource(rid);
                System.out.println("Resource deleted.");
            }
        }
    }

    // ================= 7. STREAM ANALYTICS =================
    private void handleAnalyticsMenu() throws SQLException {
        List<Task> tasks = taskService.getAllTasks();
        System.out.println("\n==================== STREAM API ANALYTICS ====================");
        System.out.printf("Total Tasks Tracked: %d\n", tasks.size());

        if (tasks.isEmpty()) {
            System.out.println("No tasks recorded yet.");
            return;
        }

        System.out.println("\n1. Category Distribution (Collectors.groupingBy):");
        analyticsEngine.countTasksByType(tasks).forEach((type, count) ->
                System.out.printf("   %-22s : %d tasks\n", type, count));

        System.out.println("\n2. Completion Partitioning (Collectors.partitioningBy):");
        var part = analyticsEngine.partitionByCompletion(tasks);
        System.out.printf("   Completed (true)  : %d tasks\n", part.getOrDefault(true, List.of()).size());
        System.out.printf("   Incomplete (false): %d tasks\n", part.getOrDefault(false, List.of()).size());

        var stats = analyticsEngine.getPayloadLengthStatistics(tasks);
        System.out.printf("\n3. Payload Statistics: Avg Length = %.1f chars | Max = %.0f chars\n",
                stats.getAverage(), stats.getMax());
        System.out.println("===============================================================");
    }
}
