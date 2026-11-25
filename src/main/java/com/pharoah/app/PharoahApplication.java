package com.pharoah.app;

import com.pharoah.agents.impl.*;
import com.pharoah.ai.AIService;
import com.pharoah.ai.OfflineHeuristicAIService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Subject;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.documents.DocumentFileProcessor;
import com.pharoah.knowledge.SubjectContextManager;
import com.pharoah.knowledge.SubjectMemory;
import com.pharoah.orchestrator.TaskOrchestrator;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.SubjectDao;
import com.pharoah.persistence.dao.TaskDao;
import com.pharoah.tasks.TaskManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Main Interactive Application Dashboard for PHAROAH.
 * 
 * Provides an enterprise console interface connecting all 14 milestones:
 * - OOP Domain & Collections Framework
 * - Multithreading & Guarded Concurrency
 * - Executor Framework & Parallel Swarms
 * - SQLite JDBC Persistence & Transactions
 * - Functional Lambdas & Stream API Analytics
 * - Generics & File Handling I/O
 * - Isolated Knowledge Spaces & 10 Specialized Agents
 */
public class PharoahApplication {

    private final DatabaseConnectionFactory dbFactory;
    private final TaskDao taskDao;
    private final SubjectDao subjectDao;
    private final TaskManager taskManager;
    private final ThreadPoolManager poolManager;
    private final SubjectContextManager contextManager;
    private final TaskAnalyticsEngine analyticsEngine;
    private final DocumentFileProcessor fileProcessor;
    private final TaskOrchestrator orchestrator;

    private String currentSubject = "JAVA";

    public PharoahApplication() {
        this.dbFactory = new DatabaseConnectionFactory("jdbc:sqlite:pharoah.db");
        this.taskDao = new TaskDao(dbFactory);
        this.subjectDao = new SubjectDao(dbFactory);
        this.taskManager = new TaskManager();
        this.poolManager = new ThreadPoolManager(Runtime.getRuntime().availableProcessors());
        this.contextManager = new SubjectContextManager();
        this.analyticsEngine = new TaskAnalyticsEngine();
        this.fileProcessor = new DocumentFileProcessor();

        this.orchestrator = new TaskOrchestrator(taskManager, poolManager, contextManager, taskDao, subjectDao);

        // Register all 10 specialized agents
        AIService aiEngine = new OfflineHeuristicAIService();
        orchestrator.registerAgent(new WriterAgent(aiEngine));
        orchestrator.registerAgent(new ResearchAgent(aiEngine));
        orchestrator.registerAgent(new PaperReaderAgent(aiEngine));
        orchestrator.registerAgent(new SummariserAgent(aiEngine));
        orchestrator.registerAgent(new ComparatorAgent(aiEngine));
        orchestrator.registerAgent(new VerificationAgent(aiEngine));
        orchestrator.registerAgent(new DataAnalystAgent(aiEngine));
        orchestrator.registerAgent(new StudyAgent(aiEngine));
        orchestrator.registerAgent(new PlannerAgent(aiEngine));
        orchestrator.registerAgent(new DocumentAnalystAgent(aiEngine));

        // Bootstrap default subject knowledge space
        contextManager.recordNote("JAVA", "Initial workspace created for Java 21 development.");
    }

    public static void main(String[] args) {
        PharoahApplication app = new PharoahApplication();

        if (args.length > 0 && args[0].equalsIgnoreCase("--demo")) {
            app.runAutomatedDemo();
        } else {
            app.startInteractiveShell();
        }
    }

    public void startInteractiveShell() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=======================================================================");
        System.out.println("   👑 PHAROAH: Multi-Agent Task Intelligence & Knowledge System        ");
        System.out.println("   Active Workspace: [" + currentSubject + "] | Engine: Java 21 LTS   ");
        System.out.println("=======================================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("\nSelect Option (1-9): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> handleTaskSubmission(scanner);
                case "2" -> handleListAgents();
                case "3" -> handleSubjectWorkspace(scanner);
                case "4" -> handleViewTaskHistory();
                case "5" -> handleStreamAnalytics();
                case "6" -> handleExportFiles();
                case "7" -> handleSwitchWorkspace(scanner);
                case "8" -> handleCrossSubjectSearch(scanner);
                case "9" -> {
                    System.out.println("\nShutting down PHAROAH cleanly...");
                    poolManager.gracefulShutdown(2);
                    running = false;
                    System.out.println("Goodbye!");
                }
                default -> System.out.println("Invalid option. Please choose between 1 and 9.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n-------------------------------------------------------------");
        System.out.println(" Current Subject Space: [" + currentSubject + "]");
        System.out.println(" [1] 📝 Submit New Intellectual Task (Concurrent Swarm)");
        System.out.println(" [2] 👥 View Swarm of 10 Registered Agents");
        System.out.println(" [3] 📚 Workspace Context: View / Add Notes");
        System.out.println(" [4] 📜 View Complete Task History (Database & Memory)");
        System.out.println(" [5] 📊 Run Stream API Analytics & Statistical Summary");
        System.out.println(" [6] 💾 Export History to CSV / Markdown Report to Disk");
        System.out.println(" [7] 🔄 Switch / Create Subject Knowledge Space");
        System.out.println(" [8] 🌐 Perform Authorized Cross-Subject Search");
        System.out.println(" [9] 🚪 Exit Application");
        System.out.println("-------------------------------------------------------------");
    }

    private void handleTaskSubmission(Scanner scanner) {
        System.out.println("\n--- Submit an Intellectual Task ---");
        System.out.println("Categories: [1] RESEARCH [2] WRITING [3] COMPARISON [4] VERIFICATION");
        System.out.println("            [5] STUDY [6] DATA ANALYSIS [7] PLANNING [8] DOCUMENT");
        System.out.print("Choose Category (1-8): ");
        String catChoice = scanner.nextLine().trim();

        TaskType type = switch (catChoice) {
            case "1" -> TaskType.RESEARCH;
            case "2" -> TaskType.WRITING;
            case "3" -> TaskType.COMPARISON;
            case "4" -> TaskType.VERIFICATION;
            case "5" -> TaskType.STUDY_ASSISTANCE;
            case "6" -> TaskType.DATA_ANALYSIS;
            case "7" -> TaskType.PLANNING;
            case "8" -> TaskType.DOCUMENT_ANALYSIS;
            default -> TaskType.RESEARCH;
        };

        System.out.print("Enter Task Title: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) title = "Untitled Query";

        System.out.print("Choose Priority ([H]igh, [M]edium, [L]ow): ");
        String pChoice = scanner.nextLine().trim().toUpperCase();
        Priority priority = switch (pChoice) {
            case "H" -> Priority.HIGH;
            case "L" -> Priority.LOW;
            default -> Priority.MEDIUM;
        };

        System.out.print("Enter Detailed Instructions / Context Payload: ");
        String payload = scanner.nextLine().trim();
        if (payload.isEmpty()) payload = title;

        Task task = new Task(title, type, priority, payload, currentSubject);
        System.out.println("\nSubmitting task to concurrent orchestrator swarm...");

        try {
            Future<TaskResult> future = orchestrator.submitTaskAsync(task);
            TaskResult result = future.get(); // Await concurrent result

            System.out.println("\n=================== TASK EXECUTION COMPLETED ===================");
            System.out.printf("Task ID: %s | Agent: %s | Duration: %d ms\n",
                    result.taskId().substring(0, 8), result.executedByAgent(), result.executionDurationMs());
            System.out.println("----------------------------------------------------------------");
            System.out.println(result.output());
            System.out.println("================================================================\n");

        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
        }
    }

    private void handleListAgents() {
        System.out.println("\n--- Swarm of Registered Agents (" + orchestrator.getRegisteredAgents().size() + ") ---");
        for (var agent : orchestrator.getRegisteredAgents()) {
            System.out.printf("  * %-20s : %s\n", agent.getName(), agent.getDescription());
        }
    }

    private void handleSubjectWorkspace(Scanner scanner) {
        SubjectMemory memory = contextManager.getOrCreateMemory(currentSubject);
        System.out.println("\n--- Subject Workspace: " + currentSubject + " ---");
        System.out.println("1. View Current Notes");
        System.out.println("2. Add New Note");
        System.out.print("Choose: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) {
            System.out.print("Enter note text: ");
            String note = scanner.nextLine().trim();
            if (!note.isEmpty()) {
                contextManager.recordNote(currentSubject, note);
                System.out.println("Note saved to workspace [" + currentSubject + "].");
            }
        } else {
            List<String> notes = memory.getNotes();
            System.out.printf("Total Notes: %d\n", notes.size());
            for (String n : notes) {
                System.out.println("  " + n);
            }
        }
    }

    private void handleViewTaskHistory() {
        System.out.println("\n--- Complete Task Audit History ---");
        List<Task> history = taskManager.getTaskHistory();
        if (history.isEmpty()) {
            System.out.println("No tasks submitted in current session yet.");
            return;
        }

        for (int i = 0; i < history.size(); i++) {
            Task t = history.get(i);
            System.out.printf("  [%d] #%s | %-16s | %-6s | Status: %-9s | Subject: %-10s | %s\n",
                    i + 1, t.getTaskId().substring(0, 8), t.getType(), t.getPriority(), t.getStatus(), t.getSubjectId(), t.getTitle());
        }
    }

    private void handleStreamAnalytics() {
        List<Task> tasks = taskManager.getTaskHistory();
        if (tasks.isEmpty()) {
            System.out.println("\nAnalytics requires at least one task. Submit tasks first!");
            return;
        }

        System.out.println("\n==================== STREAM API ANALYTICS ====================");
        System.out.println("1. Category Distribution (Collectors.groupingBy):");
        analyticsEngine.countTasksByType(tasks).forEach((type, count) ->
                System.out.printf("   %-20s : %d tasks\n", type, count));

        System.out.println("\n2. Completion Partitioning (Collectors.partitioningBy):");
        var partitioned = analyticsEngine.partitionByCompletion(tasks);
        System.out.printf("   Completed Tasks : %d\n", partitioned.get(true).size());
        System.out.printf("   Incomplete Tasks: %d\n", partitioned.get(false).size());

        System.out.println("\n3. Active Subjects (Collectors.joining):");
        System.out.println("   " + analyticsEngine.getDistinctSubjectsJoined(tasks));

        System.out.println("\n4. Input Character Length Statistics (Collectors.summarizingDouble):");
        var stats = analyticsEngine.getPayloadLengthStatistics(tasks);
        System.out.printf("   Min=%1.0f | Max=%1.0f | Avg=%1.2f chars\n",
                stats.getMin(), stats.getMax(), stats.getAverage());
        System.out.println("==============================================================\n");
    }

    private void handleExportFiles() {
        List<Task> tasks = taskManager.getTaskHistory();
        String csvOut = "reports/exported_tasks.csv";
        String mdOut = "reports/subject_" + currentSubject.toLowerCase() + "_report.md";

        try {
            fileProcessor.exportTasksToCsv(tasks, csvOut);
            System.out.println("Exported CSV to: " + csvOut);

            SubjectMemory mem = contextManager.getOrCreateMemory(currentSubject);
            fileProcessor.writeReport(mdOut, mem.synthesizeContextSummary());
            System.out.println("Exported Markdown Report to: " + mdOut);

        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    private void handleSwitchWorkspace(Scanner scanner) {
        System.out.println("\nActive Workspaces: " + contextManager.getActiveSubjects());
        System.out.print("Enter Subject Name to Switch / Create (e.g. DBMS, OS, Java, AI): ");
        String next = scanner.nextLine().trim().toUpperCase();
        if (!next.isEmpty()) {
            this.currentSubject = next;
            contextManager.getOrCreateMemory(currentSubject);
            System.out.println("Switched active workspace to: [" + currentSubject + "]");
        }
    }

    private void handleCrossSubjectSearch(Scanner scanner) {
        System.out.print("\nEnter keyword for authorized cross-subject search: ");
        String kw = scanner.nextLine().trim();
        var results = contextManager.performCrossSubjectSearch(kw);

        if (results.isEmpty()) {
            System.out.println("No matching notes found across any subject.");
        } else {
            System.out.println("Search Results Across Knowledge Spaces:");
            results.forEach((subj, notes) -> {
                System.out.println("  Workspace [" + subj + "]:");
                notes.forEach(n -> System.out.println("    * " + n));
            });
        }
    }

    /**
     * Automated end-to-end demo walkthrough for testing and demonstration.
     */
    public void runAutomatedDemo() {
        System.out.println("=======================================================================");
        System.out.println("   👑 PHAROAH: AUTOMATED END-TO-END DEMONSTRATION RUN                 ");
        System.out.println("=======================================================================");

        try {
            // Task 1: Comparison
            Task t1 = new Task("ZGC vs G1GC", TaskType.COMPARISON, Priority.HIGH,
                    "Sub-millisecond latency vs maximum throughput", "JAVA");
            System.out.println("\n[DEMO] Submitting Task 1: " + t1.getTitle());
            TaskResult r1 = orchestrator.submitTaskAsync(t1).get();
            System.out.printf("  Completed by %s in %d ms\n", r1.executedByAgent(), r1.executionDurationMs());

            // Task 2: Study Aid
            Task t2 = new Task("Two-Phase Locking (2PL)", TaskType.STUDY_ASSISTANCE, Priority.HIGH,
                    "Growing and shrinking phase serialization", "DBMS");
            System.out.println("\n[DEMO] Submitting Task 2: " + t2.getTitle());
            TaskResult r2 = orchestrator.submitTaskAsync(t2).get();
            System.out.printf("  Completed by %s in %d ms\n", r2.executedByAgent(), r2.executionDurationMs());

            // Run Stream Analytics
            handleStreamAnalytics();

            // Export to files
            handleExportFiles();

            poolManager.gracefulShutdown(2);
            System.out.println("\n🎉 AUTOMATED DEMO COMPLETE: ALL MILESTONES INTEGRATED PERFECTLY!");

        } catch (Exception e) {
            System.err.println("Demo encountered exception: " + e.getMessage());
        }
    }
}
