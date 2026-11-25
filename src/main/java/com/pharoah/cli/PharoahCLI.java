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
import com.pharoah.document.Document;
import com.pharoah.document.DocumentSection;
import com.pharoah.document.DocumentType;
import com.pharoah.document.DocumentWorkflowService;
import com.pharoah.knowledge.KnowledgeResource;
import com.pharoah.knowledge.KnowledgeService;
import com.pharoah.knowledge.ResourceType;
import com.pharoah.orchestrator.TaskOrchestrator;
import com.pharoah.project.Project;
import com.pharoah.project.ProjectService;
import com.pharoah.tasks.TaskService;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Future;

// SANCHIT: Implemented PharoahCLI.
// The primary terminal-based user interface connecting all project modules,
// interactive menus, sub-commands, human review gates, and progress tracking.
public class PharoahCLI {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final AgentService agentService;
    private final TaskOrchestrator orchestrator;
    private final KnowledgeService knowledgeService;
    private final DocumentWorkflowService documentService;
    private final TaskAnalyticsEngine analyticsEngine;
    private final Scanner scanner;

    public PharoahCLI(ProjectService projectService,
                      TaskService taskService,
                      AgentService agentService,
                      TaskOrchestrator orchestrator,
                      KnowledgeService knowledgeService,
                      DocumentWorkflowService documentService,
                      TaskAnalyticsEngine analyticsEngine) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.agentService = agentService;
        this.orchestrator = orchestrator;
        this.knowledgeService = knowledgeService;
        this.documentService = documentService;
        this.analyticsEngine = analyticsEngine;
        this.scanner = new Scanner(System.in);
    }

    // SANCHIT: Main terminal interactive shell loop
    public void startShell() {
        printBanner();

        boolean running = true;
        while (running) {
            Project active = projectService.getActiveProject();
            String activeName = (active != null) ? active.getName() : "None";

            System.out.println("\n=========================================================================");
            System.out.printf("  👑 PHAROAH TERMINAL WORKSPACE | Active Project: [%s]\n", activeName);
            System.out.println("=========================================================================");
            System.out.println("  [1] 📁 Project Management     - Create, list, switch, and manage projects");
            System.out.println("  [2] 📋 Task Management        - Create, assign, review, and track tasks");
            System.out.println("  [3] 👥 Agent Swarm Management - View, configure, and register specialized agents");
            System.out.println("  [4] ⚡ Central Orchestration   - Decompose objectives, run concurrent workflows");
            System.out.println("  [5] 📚 Knowledge Repository   - Save URLs, notes, code snippets, search assets");
            System.out.println("  [6] 📄 Document Workflows     - Research Papers, Blogs, Tech Docs, Proposals");
            System.out.println("  [7] 📊 Stream API Analytics   - Live workload distribution & completion stats");
            System.out.println("  [8] 🚪 Exit PHAROAH");
            System.out.println("=========================================================================");
            System.out.print("Select an option (1-8): ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> handleProjectMenu();
                    case "2" -> handleTaskMenu();
                    case "3" -> handleAgentMenu();
                    case "4" -> handleOrchestrationMenu();
                    case "5" -> handleKnowledgeMenu();
                    case "6" -> handleDocumentMenu();
                    case "7" -> handleAnalyticsMenu();
                    case "8" -> {
                        System.out.println("\n[PHAROAH] Shutting down cleanly. All persistent data saved to MySQL.");
                        running = false;
                    }
                    default -> System.out.println("Invalid option. Please enter a number between 1 and 8.");
                }
            } catch (Exception e) {
                System.err.println("\n[ERROR] An exception occurred: " + e.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("""
            =========================================================================
               👑 PHAROAH: Terminal-Based Multi-Agent Orchestration & Knowledge System
               Engine: Java 21 LTS | Persistence: MySQL 8.0 (JDBC) | Interface: Terminal
               Developers: Varij (Agents & Orchestration) | Ashutosh (JDBC & MySQL) | Sanchit (Projects & Docs)
            =========================================================================
            """);
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
                    System.out.printf("  [%d] %s (%s)\n", i + 1, list.get(i).getName(), list.get(i).getProjectId().substring(0, 8));
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
                System.out.println("Task created: " + t.getTaskId().substring(0, 8) + " (Status: " + t.getStatus() + ")");
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
                // Human-in-the-loop review
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
                System.out.println("Agent registered successfully: " + created.getName() + " (" + created.getAgentId().substring(0, 8) + ")");
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
                System.out.print("Enter Domain Topic (e.g. Virtual Memory in OS, AI in Cybersecurity): ");
                String topic = scanner.nextLine().trim();

                System.out.println("\nDecomposing objective into structured subtasks with dependency graph...");
                List<Task> subtasks = orchestrator.decomposeObjective(pid, title, topic);
                System.out.printf("Successfully generated and saved %d subtasks:\n", subtasks.size());
                for (int i = 0; i < subtasks.size(); i++) {
                    Task t = subtasks.get(i);
                    System.out.printf("  [%d] #%s | %s (Prerequisite: %s)\n",
                            i + 1, t.getTaskId().substring(0, 8), t.getTitle(),
                            t.getDependsOnTaskId() != null ? t.getDependsOnTaskId().substring(0, 8) : "NONE (First)");
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
                TaskResult res = future.get(); // Await concurrent result

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
                System.out.println("Knowledge asset saved successfully (ID: " + r.getResourceId().substring(0, 8) + ")");
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

    // ================= 6. DOCUMENT WORKFLOWS =================
    private void handleDocumentMenu() throws SQLException {
        Project active = projectService.getActiveProject();
        String pid = (active != null) ? active.getProjectId() : "GLOBAL";

        System.out.println("\n--- [MODULE F] DOCUMENT WORKFLOW MANAGEMENT ---");
        System.out.println("1. Create New Document from Template (Research Paper, Blog, Tech Doc, Proposal)");
        System.out.println("2. View Document & Its Sections");
        System.out.println("3. Assign Agent to Document Section");
        System.out.println("4. Draft Section Content via Agent");
        System.out.println("5. Human Review of Section (Approve / Reject)");
        System.out.println("6. Compile Approved Sections into Final Document");
        System.out.print("Choose (1-6): ");
        String sub = scanner.nextLine().trim();

        switch (sub) {
            case "1" -> {
                System.out.print("Document Title: ");
                String title = scanner.nextLine().trim();
                System.out.println("Choose Format:");
                System.out.println("  [1] Academic Research Paper (Abstract, Intro, Lit Review, Methodology, Results, Conclusion)");
                System.out.println("  [2] Technical Blog Post (Hook, Concepts, Code Implementation, Best Practices)");
                System.out.println("  [3] Technical Documentation (Overview, Architecture, Install, Config, API Docs)");
                System.out.println("  [4] Business Proposal (Executive Summary, Problem, Solution, Timeline, Financials)");
                System.out.print("Format (1-4): ");
                DocumentType type = switch (scanner.nextLine().trim()) {
                    case "2" -> DocumentType.TECHNICAL_BLOG;
                    case "3" -> DocumentType.TECHNICAL_DOCUMENTATION;
                    case "4" -> DocumentType.BUSINESS_PROPOSAL;
                    default -> DocumentType.RESEARCH_PAPER;
                };

                Document doc = documentService.createDocumentWithTemplate(pid, title, type);
                System.out.println("\nDocument created: " + doc.getTitle());
                System.out.printf("Generated %d standard template sections ready for agent delegation!\n", type.getStandardSections().size());
            }
            case "2" -> {
                List<Document> docs = documentService.getDocumentsByProject(pid);
                if (docs.isEmpty()) {
                    System.out.println("No documents in active project.");
                    return;
                }
                for (int i = 0; i < docs.size(); i++) {
                    System.out.printf("  [%d] %s (ID: %s)\n", i + 1, docs.get(i).getTitle(), docs.get(i).getDocumentId().substring(0, 8));
                }
                System.out.print("Select document number: ");
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx >= 0 && idx < docs.size()) {
                    Document doc = docs.get(idx);
                    List<DocumentSection> sections = documentService.getSections(doc.getDocumentId());
                    System.out.printf("\nSections for '%s':\n", doc.getTitle());
                    for (DocumentSection s : sections) {
                        System.out.println("  " + s);
                    }
                }
            }
            case "3" -> {
                System.out.print("Enter Section ID: ");
                String sid = scanner.nextLine().trim();
                System.out.print("Enter Agent ID to assign: ");
                String aid = scanner.nextLine().trim();
                documentService.assignAgentToSection(sid, aid);
                System.out.println("Agent assigned to section.");
            }
            case "4" -> {
                System.out.print("Enter Section ID to draft: ");
                String sid = scanner.nextLine().trim();
                DocumentSection sec = documentService.getSectionById(sid);
                if (sec == null) {
                    System.out.println("Section not found.");
                    return;
                }
                System.out.printf("Drafting section '%s' using assigned/recommended agent...\n", sec.getSectionName());
                String simulatedDraft = String.format("""
                    This section systematically addresses the requirements of %s.
                    Through architectural decomposition, verifiable specifications, and robust
                    implementation patterns, the system satisfies performance and consistency bounds.
                    """, sec.getSectionName());

                documentService.updateSectionContent(sid, simulatedDraft);
                System.out.println("Section draft generated and submitted for human review (Status: UNDER_REVIEW).");
            }
            case "5" -> {
                System.out.print("Enter Section ID to review: ");
                String sid = scanner.nextLine().trim();
                DocumentSection sec = documentService.getSectionById(sid);
                if (sec == null) {
                    System.out.println("Section not found.");
                    return;
                }
                System.out.println("\n--- SECTION DRAFT REVIEW ---");
                System.out.println("Section: " + sec.getSectionName());
                System.out.println("Content:\n" + sec.getContent());
                System.out.println("----------------------------");
                System.out.print("Approve this section draft? (y/n): ");
                boolean app = scanner.nextLine().trim().equalsIgnoreCase("y");
                documentService.reviewSection(sid, app);
                System.out.println("Section status updated to: " + (app ? "APPROVED" : "REJECTED"));
            }
            case "6" -> {
                System.out.print("Enter Document ID to compile: ");
                String did = scanner.nextLine().trim();
                String compiled = documentService.compileDocument(did);
                System.out.println("\n==================== COMPILED FINAL DOCUMENT ====================");
                System.out.println(compiled);
                System.out.println("=================================================================");
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

        // Collectors.groupingBy
        System.out.println("\n1. Category Distribution (Collectors.groupingBy):");
        analyticsEngine.countTasksByType(tasks).forEach((type, count) ->
                System.out.printf("   %-22s : %d tasks\n", type, count));

        // Collectors.partitioningBy
        System.out.println("\n2. Completion Partitioning (Collectors.partitioningBy):");
        var part = analyticsEngine.partitionByCompletion(tasks);
        System.out.printf("   Completed (true)  : %d tasks\n", part.getOrDefault(true, List.of()).size());
        System.out.printf("   Incomplete (false): %d tasks\n", part.getOrDefault(false, List.of()).size());

        // Collectors.summarizingDouble
        var stats = analyticsEngine.getPayloadLengthStatistics(tasks);
        System.out.printf("\n3. Payload Statistics: Avg Length = %.1f chars | Max = %.0f chars\n",
                stats.getAverage(), stats.getMax());
        System.out.println("===============================================================");
    }
}
