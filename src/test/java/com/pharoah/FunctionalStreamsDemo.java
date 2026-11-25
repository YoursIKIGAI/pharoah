package com.pharoah;

import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Educational Demonstration and Test Suite for Milestones 7 and 8:
 * "Lambdas, Functional Interfaces, and Stream API Analytics"
 */
public class FunctionalStreamsDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   👑 PHAROAH - MILESTONES 7 & 8: STREAMS & LAMBDAS SUITE     ");
        System.out.println("===============================================================\n");

        TaskAnalyticsEngine analytics = new TaskAnalyticsEngine();

        // -------------------------------------------------------------
        // Step 1: Supplier & Test Data Generation
        // -------------------------------------------------------------
        System.out.println("--- [1] Generating Dataset using Functional Supplier ---");
        Supplier<List<Task>> taskDataSupplier = () -> {
            Task t1 = new Task("ZGC vs G1GC in Java 21", TaskType.RESEARCH, Priority.HIGH, "Detailed latency analysis.", "Java");
            t1.markCompleted("G1 is throughput-optimized; ZGC delivers sub-millisecond pauses.");

            Task t2 = new Task("B-Trees in Database Storage", TaskType.STUDY_ASSISTANCE, Priority.MEDIUM, "Disk-based node balancing.", "DBMS");
            t2.markCompleted("B-Trees minimize expensive random disk seeks.");

            Task t3 = new Task("Deadlock Avoidance Banker's Algo", TaskType.STUDY_ASSISTANCE, Priority.HIGH, "Safe sequence derivation.", "Operating Systems");

            Task t4 = new Task("REST vs gRPC Latency Benchmarking", TaskType.COMPARISON, Priority.LOW, "Protocol buffer vs JSON serialization.", "Computer Networks");

            Task t5 = new Task("Literature Review on Transformer Attention", TaskType.RESEARCH, Priority.HIGH, "Multi-head attention mechanisms.", "Artificial Intelligence");
            t5.markCompleted("Self-attention complexity is O(N^2) where N is sequence length.");

            return List.of(t1, t2, t3, t4, t5);
        };

        List<Task> tasks = taskDataSupplier.get();
        System.out.printf("  Loaded %d tasks across 5 distinct subjects.\n\n", tasks.size());

        // -------------------------------------------------------------
        // Step 2: Predicate Chaining (.and(), .negate())
        // -------------------------------------------------------------
        System.out.println("--- [2] Filtering with Chained Predicates ---");
        Predicate<Task> highPriorityAndCompleted = TaskAnalyticsEngine.IS_HIGH_PRIORITY
                .and(TaskAnalyticsEngine.IS_COMPLETED);

        List<Task> highCompletedTasks = analytics.filterTasks(tasks, highPriorityAndCompleted);
        System.out.printf("  Tasks matching (HIGH Priority AND COMPLETED): %d\n", highCompletedTasks.size());
        for (Task t : highCompletedTasks) {
            System.out.println("    * " + t.getTitle() + " (Subject: " + t.getSubjectId() + ")");
        }
        assert highCompletedTasks.size() == 2 : "Expected 2 tasks matching criteria";
        System.out.println("  >> PASSED: Predicate chaining verified.\n");

        // -------------------------------------------------------------
        // Step 3: Stream groupingBy (Task Type Frequency)
        // -------------------------------------------------------------
        System.out.println("--- [3] Stream groupingBy: Tasks by Category ---");
        Map<TaskType, Long> typeCounts = analytics.countTasksByType(tasks);
        typeCounts.forEach((type, count) ->
                System.out.printf("  Category: %-20s -> Count: %d\n", type, count));
        assert typeCounts.get(TaskType.RESEARCH) == 2 : "Expected 2 research tasks";
        System.out.println("  >> PASSED: Collectors.groupingBy frequency counting verified.\n");

        // -------------------------------------------------------------
        // Step 4: Stream partitioningBy (Completed vs Pending)
        // -------------------------------------------------------------
        System.out.println("--- [4] Stream partitioningBy: Completed vs In-Progress ---");
        Map<Boolean, List<Task>> partitioned = analytics.partitionByCompletion(tasks);
        System.out.printf("  Completed (true) : %d tasks\n", partitioned.get(true).size());
        System.out.printf("  Pending   (false): %d tasks\n", partitioned.get(false).size());
        assert partitioned.get(true).size() == 3 : "Expected 3 completed tasks";
        assert partitioned.get(false).size() == 2 : "Expected 2 pending tasks";
        System.out.println("  >> PASSED: Collectors.partitioningBy verified.\n");

        // -------------------------------------------------------------
        // Step 5: Stream Collectors.joining()
        // -------------------------------------------------------------
        System.out.println("--- [5] Stream Collectors.joining() ---");
        String subjectsList = analytics.getDistinctSubjectsJoined(tasks);
        System.out.println("  Distinct Subjects in System: " + subjectsList);
        assert subjectsList.contains("Java") && subjectsList.contains("DBMS") : "Subject joining missing entries";
        System.out.println("  >> PASSED: Collectors.joining verified.\n");

        // -------------------------------------------------------------
        // Step 6: Stream summarizingDouble (Statistical Analysis)
        // -------------------------------------------------------------
        System.out.println("--- [6] Stream summarizingDouble: Payload Statistics ---");
        DoubleSummaryStatistics stats = analytics.getPayloadLengthStatistics(tasks);
        System.out.printf("  Payload Lengths: Min=%.0f, Max=%.0f, Avg=%.2f, TotalChars=%.0f\n",
                stats.getMin(), stats.getMax(), stats.getAverage(), stats.getSum());
        assert stats.getCount() == 5 : "Stats count should be 5";
        System.out.println("  >> PASSED: Statistical summarizingDouble verified.\n");

        // -------------------------------------------------------------
        // Step 7: Stream reduce() pipeline
        // -------------------------------------------------------------
        System.out.println("--- [7] Stream reduce(): Title Concatenation ---");
        String reduced = analytics.concatenateTitlesWithReduce(tasks);
        System.out.println("  Combined Title Pipeline: " + reduced);
        assert !reduced.isEmpty() : "Reduce should produce non-empty string";
        System.out.println("  >> PASSED: Stream reduce() verified.\n");

        System.out.println("===============================================================");
        System.out.println("  🎉 MILESTONES 7 & 8 PASSED: FUNCTIONAL STREAMS COMPLETE!    ");
        System.out.println("===============================================================");
    }
}
