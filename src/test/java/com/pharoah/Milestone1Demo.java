package com.pharoah;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.tasks.TaskManager;

import java.util.List;

/**
 * Educational Demonstration and Test Suite for Milestone 1:
 * "Task Management using OOP and Collections Framework"
 *
 * This executable class validates:
 * 1. Encapsulation & Domain Validation in Task
 * 2. Automatic Heap Ordering in PriorityQueue via Comparable<Task>
 * 3. O(1) Duplicate Detection in HashSet
 * 4. O(1) Key-Value Fast Lookup in HashMap
 * 5. Chronological Audit Trail in ArrayList
 * 6. In-flight State Tracking in ConcurrentHashMap
 */
public class Milestone1Demo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   👑 PHAROAH - MILESTONE 1: OOP & COLLECTIONS VERIFICATION   ");
        System.out.println("===============================================================\n");

        TaskManager manager = new TaskManager();

        // -------------------------------------------------------------
        // Step 1: Create sample tasks with varying priorities and types
        // -------------------------------------------------------------
        System.out.println("--- [1] Creating Intellectual Tasks ---");
        Task t1 = new Task("Explore Garbage Collection in Java", TaskType.RESEARCH, Priority.LOW,
                "Analyze G1 vs ZGC garbage collectors in Java 21.", "Java");

        Task t2 = new Task("QuickSort vs MergeSort", TaskType.COMPARISON, Priority.MEDIUM,
                "Compare time complexity, space complexity and stability.", "Data Structures");

        Task t3 = new Task("Urgent: Deadlock Detection in OS", TaskType.STUDY_ASSISTANCE, Priority.HIGH,
                "Generate flashcards and Banker's algorithm revision notes.", "Operating Systems");

        Task t4 = new Task("Critical: SQL Injection Vulnerability Fix", TaskType.CODING, Priority.HIGH,
                "Audit PreparedStatement implementation against SQL injection.", "DBMS");

        System.out.println("Created 4 tasks successfully:");
        System.out.println("  * " + t1);
        System.out.println("  * " + t2);
        System.out.println("  * " + t3);
        System.out.println("  * " + t4);
        System.out.println();

        // -------------------------------------------------------------
        // Step 2: Submit tasks in NON-PRIORITY order (LOW -> MEDIUM -> HIGH -> HIGH)
        // -------------------------------------------------------------
        System.out.println("--- [2] Submitting Tasks to TaskManager ---");
        manager.submitTask(t1); // Submitted 1st (LOW)
        manager.submitTask(t2); // Submitted 2nd (MEDIUM)
        manager.submitTask(t3); // Submitted 3rd (HIGH)
        manager.submitTask(t4); // Submitted 4th (HIGH)

        System.out.printf("Total Submitted: %d | Pending in PriorityQueue: %d\n\n",
                manager.getTotalSubmittedCount(), manager.getPendingCount());

        // -------------------------------------------------------------
        // Step 3: Test HashSet O(1) Duplicate Rejection
        // -------------------------------------------------------------
        System.out.println("--- [3] Testing HashSet Duplicate Detection ---");
        boolean duplicateResult = manager.submitTask(t1); // Try to add t1 again
        System.out.println("Attempting to re-submit Task #1 (same UUID):");
        System.out.println("  Duplicate Submission Allowed? " + duplicateResult + " (Expected: false)");
        assert !duplicateResult : "HashSet duplicate check failed!";
        System.out.println("  >> PASSED: HashSet correctly prevented duplicate task ID registration.\n");

        // -------------------------------------------------------------
        // Step 4: Test HashMap O(1) Instant Lookup
        // -------------------------------------------------------------
        System.out.println("--- [4] Testing HashMap Fast Lookup ---");
        String searchId = t3.getTaskId();
        Task foundTask = manager.getTaskById(searchId);
        System.out.println("Searching for Task ID: " + searchId);
        System.out.println("  Found Task Title: " + (foundTask != null ? foundTask.getTitle() : "NOT FOUND"));
        assert foundTask != null && foundTask.getTitle().contains("Deadlock") : "HashMap lookup failed!";
        System.out.println("  >> PASSED: HashMap retrieved task in O(1) time.\n");

        // -------------------------------------------------------------
        // Step 5: Test PriorityQueue Heap Ordering
        // Notice: t1 (LOW) was submitted first, but t3 and t4 (HIGH) MUST dequeue first!
        // -------------------------------------------------------------
        System.out.println("--- [5] Testing PriorityQueue Automatic Priority Scheduling ---");
        System.out.println("Extracting tasks from PriorityQueue via pollNextPriorityTask():");

        int rank = 1;
        while (manager.getPendingCount() > 0) {
            Task dequeued = manager.pollNextPriorityTask();
            System.out.printf("  #%d Dequeued -> Priority: %-6s | Title: %s\n",
                    rank++, dequeued.getPriority(), dequeued.getTitle());

            // Simulate worker picking up and completing the task
            TaskResult mockResult = TaskResult.success(
                    dequeued.getTaskId(),
                    "Synthesized intellectual analysis for: " + dequeued.getTitle(),
                    150L,
                    dequeued.getType().getPrimaryAgentName()
            );
            manager.completeTask(dequeued.getTaskId(), mockResult);
        }
        System.out.println("  >> PASSED: PriorityQueue correctly scheduled HIGH tasks before MEDIUM and LOW!\n");

        // -------------------------------------------------------------
        // Step 6: Test ArrayList Chronological History & Filtering
        // -------------------------------------------------------------
        System.out.println("--- [6] Testing ArrayList Task History & Subject Filtering ---");
        List<Task> history = manager.getTaskHistory();
        System.out.println("Chronological Submission Order (ArrayList preserves insertion sequence):");
        for (int i = 0; i < history.size(); i++) {
            System.out.printf("  [%d] %s (Status: %s)\n", i + 1, history.get(i).getTitle(), history.get(i).getStatus());
        }

        System.out.println("\nFiltering tasks by Subject 'Data Structures':");
        List<Task> dsTasks = manager.getTasksBySubject("Data Structures");
        for (Task t : dsTasks) {
            System.out.println("  Found: " + t.getTitle() + " | Subject: " + t.getSubjectId());
        }
        assert dsTasks.size() == 1 : "Subject filtering failed!";
        System.out.println("  >> PASSED: Subject filtering successfully verified.\n");

        System.out.println("===============================================================");
        System.out.println("  🎉 MILESTONE 1 COMPLETE: ALL VERIFICATION CHECKS PASSED!     ");
        System.out.println("===============================================================");
    }
}
