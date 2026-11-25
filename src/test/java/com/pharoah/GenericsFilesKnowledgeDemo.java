package com.pharoah;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.documents.DocumentFileProcessor;
import com.pharoah.knowledge.SubjectContextManager;
import com.pharoah.knowledge.SubjectMemory;
import com.pharoah.persistence.repository.InMemoryGenericRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Educational Demonstration and Test Suite for Milestones 9 and 10:
 * "Java Generics, File Handling & I/O, and Subject Knowledge Spaces"
 */
public class GenericsFilesKnowledgeDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("===============================================================");
        System.out.println("   👑 PHAROAH - MILESTONES 9 & 10: GENERICS, FILES & SPACES   ");
        System.out.println("===============================================================\n");

        // =============================================================
        // PART 1 (MILESTONE 9): Java Generics & Bounded Wildcards
        // =============================================================
        System.out.println("--- [PART 1] Verifying Generic Repository & Bounded Wildcards ---");

        InMemoryGenericRepository<Task, String> taskRepo =
                new InMemoryGenericRepository<>(Task::getTaskId);

        Task gt1 = new Task("Generics & Type Erasure", TaskType.STUDY_ASSISTANCE, Priority.HIGH, "Invariant vs Covariant arrays.");
        Task gt2 = new Task("Bounded Wildcards in Java", TaskType.STUDY_ASSISTANCE, Priority.HIGH, "PECS: Producer Extends, Consumer Super.");

        // Demonstrates Bounded Wildcard: saveAll(Collection<? extends Task>)
        List<Task> initialTasks = List.of(gt1, gt2);
        taskRepo.saveAll(initialTasks);

        System.out.printf("  Repository total entity count: %d\n", taskRepo.count());
        assert taskRepo.count() == 2 : "Repository count should be 2";

        // Demonstrates Generic Method with mapper: <R> List<R> mapAll(Function<? super T, ? extends R>)
        List<String> titles = taskRepo.mapAll(Task::getTitle);
        System.out.println("  Mapped Entity Titles (Generic Method): " + titles);
        assert titles.size() == 2 : "Titles size should be 2";
        System.out.println("  >> PASSED: Generic Repository, bounded wildcards, and mapping verified.\n");

        // =============================================================
        // PART 2 (MILESTONE 9): File Handling (Byte/Char Streams & Tokenizer)
        // =============================================================
        System.out.println("--- [PART 2] Verifying File I/O (BufferedReader, Tokenizer, CSV) ---");
        DocumentFileProcessor fileProcessor = new DocumentFileProcessor();

        String reportPath = "target/test_output/sample_report.md";
        String sampleReportContent = """
            # PHAROAH Study Report
            Subject: Operating Systems
            Topic: Virtual Memory 2026
            Score: 98.5
            Status: Passed
        """;

        // Write report using BufferedWriter
        fileProcessor.writeReport(reportPath, sampleReportContent);
        System.out.println("  Written report to: " + reportPath);

        // Read report back using BufferedReader
        String readBack = fileProcessor.readTextFile(reportPath);
        assert readBack.contains("Virtual Memory 2026") : "File content mismatch";
        System.out.println("  Read back verified via BufferedReader.");

        // Tokenize words and numbers using StreamTokenizer
        List<String> tokens = fileProcessor.tokenizeWords(reportPath);
        System.out.println("  StreamTokenizer extracted tokens: " + tokens);
        assert tokens.contains("NUM:2026.0") : "StreamTokenizer failed to extract number 2026";
        System.out.println("  >> PASSED: StreamTokenizer tokenized words and numbers.\n");

        // Export tasks to CSV
        String csvPath = "target/test_output/exported_tasks.csv";
        fileProcessor.exportTasksToCsv(initialTasks, csvPath);
        String csvContent = fileProcessor.readTextFile(csvPath);
        System.out.println("  Exported Tasks CSV preview:\n" + csvContent);
        assert csvContent.contains("Bounded Wildcards in Java") : "CSV export missing task";
        System.out.println("  >> PASSED: CSV export and file handling verified.\n");

        // =============================================================
        // PART 3 (MILESTONE 10): Knowledge Spaces & Strict Isolation
        // =============================================================
        System.out.println("--- [PART 3] Verifying Knowledge Spaces & Subject Isolation ---");
        SubjectContextManager contextManager = new SubjectContextManager();

        // 1. Record notes strictly inside Java workspace
        contextManager.recordNote("Java", "Java 21 Virtual Threads (Project Loom) provide lightweight concurrency.");
        contextManager.recordNote("Java", "Garbage Collection tuning: ZGC achieves sub-millisecond pause times.");

        // 2. Record notes strictly inside DBMS workspace
        contextManager.recordNote("DBMS", "B-Tree index structure keeps data sorted and allows search in O(log n).");
        contextManager.recordNote("DBMS", "ACID properties: Atomicity, Consistency, Isolation, Durability.");

        // 3. Inspect Java workspace memory
        SubjectMemory javaMemory = contextManager.getOrCreateMemory("Java");
        System.out.println("  Java Workspace Memory Notes Count: " + javaMemory.getNotes().size());
        for (String note : javaMemory.getNotes()) {
            System.out.println("    [JAVA NOTE] " + note);
            assert !note.contains("ACID") : "LEAK DETECTED: DBMS note leaked into Java context!";
        }

        // 4. Inspect DBMS workspace memory
        SubjectMemory dbmsMemory = contextManager.getOrCreateMemory("DBMS");
        System.out.println("  DBMS Workspace Memory Notes Count: " + dbmsMemory.getNotes().size());
        for (String note : dbmsMemory.getNotes()) {
            System.out.println("    [DBMS NOTE] " + note);
            assert !note.contains("Virtual Threads") : "LEAK DETECTED: Java note leaked into DBMS context!";
        }
        System.out.println("  >> PASSED: Strict subject memory isolation verified.\n");

        // 5. Test Unauthorized Isolation Access Guard
        System.out.println("  Testing unauthorized context cross-talk detection...");
        boolean isolationBlocked = false;
        try {
            // Attempt to access DBMS from Java without cross-subject permission
            contextManager.validateIsolation("Java", "DBMS", false);
        } catch (IllegalStateException e) {
            isolationBlocked = true;
            System.out.println("  Caught expected violation: " + e.getMessage());
        }
        assert isolationBlocked : "Isolation validator failed to block unauthorized access!";
        System.out.println("  >> PASSED: Unauthorized context switching blocked.\n");

        // 6. Test Authorized Cross-Subject Search
        System.out.println("  Testing authorized cross-subject search for 'Concurrency' / 'Threads'...");
        Map<String, List<String>> searchResults = contextManager.performCrossSubjectSearch("Threads");
        System.out.println("  Cross-subject query returned matches in subjects: " + searchResults.keySet());
        assert searchResults.containsKey("JAVA") && !searchResults.containsKey("DBMS") : "Cross-subject search incorrect";
        System.out.println("  >> PASSED: Cross-subject search verified.\n");

        System.out.println("===============================================================");
        System.out.println("  🎉 MILESTONES 9 & 10 PASSED: GENERICS, FILES & SPACES READY! ");
        System.out.println("===============================================================");
    }
}
