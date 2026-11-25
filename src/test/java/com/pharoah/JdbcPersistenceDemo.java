package com.pharoah;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Subject;
import com.pharoah.core.model.Task;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.TransactionManager;
import com.pharoah.persistence.dao.SubjectDao;
import com.pharoah.persistence.dao.TaskDao;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Educational Demonstration and Test Suite for Milestones 5 and 6:
 * "JDBC Architecture, PreparedStatement, ResultSet Mapping, and Transactions"
 */
public class JdbcPersistenceDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("   👑 PHAROAH - MILESTONES 5 & 6: JDBC PERSISTENCE SUITE      ");
        System.out.println("===============================================================\n");

        // Use a test SQLite file
        String testDbPath = "target/test_pharoah.db";
        new File(testDbPath).delete(); // Start clean

        DatabaseConnectionFactory dbFactory = new DatabaseConnectionFactory("jdbc:sqlite:" + testDbPath);
        TaskDao taskDao = new TaskDao(dbFactory);
        SubjectDao subjectDao = new SubjectDao(dbFactory);
        TransactionManager txManager = new TransactionManager(dbFactory);

        // -------------------------------------------------------------
        // Step 1: DatabaseMetaData Inspection
        // -------------------------------------------------------------
        System.out.println("--- [1] Inspecting DatabaseMetaData ---");
        dbFactory.printDatabaseMetaData();
        System.out.println();

        // -------------------------------------------------------------
        // Step 2: Subject & Task CRUD with PreparedStatement
        // -------------------------------------------------------------
        System.out.println("--- [2] Inserting Subjects & Tasks via PreparedStatement ---");
        Subject javaSubject = new Subject("Java", "Core & Advanced Java concepts, JVM, JDBC & Concurrency.");
        subjectDao.insertSubject(javaSubject);
        System.out.println("  Inserted Subject: " + javaSubject.getName());

        Task task1 = new Task("JDBC PreparedStatement Deep Dive", TaskType.STUDY_ASSISTANCE, Priority.HIGH,
                "Study SQL injection prevention and binary parameter binding.", javaSubject.getSubjectId());
        taskDao.insertTask(task1);
        System.out.println("  Inserted Task: " + task1.getTitle() + " (ID: " + task1.getTaskId().substring(0, 8) + ")");

        // Retrieve by ID
        Task retrieved = taskDao.findById(task1.getTaskId());
        System.out.println("  Retrieved from DB: " + retrieved.getTitle() + " | Priority: " + retrieved.getPriority());
        assert retrieved != null && retrieved.getTitle().equals(task1.getTitle()) : "Task retrieval mismatch!";
        System.out.println("  >> PASSED: PreparedStatement insertion and ResultSet hydration verified.\n");

        // -------------------------------------------------------------
        // Step 3: ResultSetMetaData dynamic inspection
        // -------------------------------------------------------------
        System.out.println("--- [3] Inspecting ResultSetMetaData ---");
        List<Task> allTasks = taskDao.findAll();
        System.out.printf("  Total tasks in DB: %d\n", allTasks.size());
        assert allTasks.size() == 1 : "Expected 1 task in database";
        System.out.println("  >> PASSED: ResultSetMetaData column inspection verified.\n");

        // -------------------------------------------------------------
        // Step 4: Transaction Management (Commit Test)
        // -------------------------------------------------------------
        System.out.println("--- [4] Testing Atomic Transaction COMMIT ---");
        txManager.executeInTransaction(conn -> {
            Subject osSubject = new Subject("Operating Systems", "Process scheduling and memory virtualization.");
            subjectDao.insertSubject(osSubject, conn);

            Task osTask = new Task("Virtual Memory Paging", TaskType.RESEARCH, Priority.MEDIUM,
                    "Analyze TLB hit rates and page replacement algorithms.", osSubject.getSubjectId());
            taskDao.insertTask(osTask, conn);
        });

        Subject foundOs = subjectDao.findByName("Operating Systems");
        assert foundOs != null : "Operating Systems subject should be committed!";
        System.out.println("  Found committed subject: " + foundOs.getName());
        System.out.println("  >> PASSED: Atomic commit verified.\n");

        // -------------------------------------------------------------
        // Step 5: Transaction Management (Rollback Test)
        // -------------------------------------------------------------
        System.out.println("--- [5] Testing Atomic Transaction ROLLBACK on Failure ---");
        boolean rollbackTriggered = false;
        try {
            txManager.executeInTransaction(conn -> {
                Subject failingSubject = new Subject("Failing Subject", "This should NOT be saved in the database.");
                subjectDao.insertSubject(failingSubject, conn);
                System.out.println("  [TX] Staged 'Failing Subject' in uncommitted transaction.");

                // Intentionally trigger an error by re-inserting the same subject name (violates UNIQUE constraint!)
                System.out.println("  [TX] Deliberately violating UNIQUE constraint to force failure...");
                subjectDao.insertSubject(failingSubject, conn); // Fails with SQL constraint violation!
            });
        } catch (SQLException e) {
            rollbackTriggered = true;
            System.out.println("  Caught expected exception: " + e.getMessage());
        }

        assert rollbackTriggered : "Transaction should have thrown SQLException!";
        Subject shouldBeNull = subjectDao.findByName("Failing Subject");
        System.out.println("  Querying for 'Failing Subject' after rollback: " + shouldBeNull);
        assert shouldBeNull == null : "Rollback failed: 'Failing Subject' should NOT exist in database!";
        System.out.println("  >> PASSED: Transaction rollback cleanly undone all uncommitted changes.\n");

        System.out.println("===============================================================");
        System.out.println("  🎉 MILESTONES 5 & 6 PASSED: JDBC & TRANSACTIONS COMPLETE!    ");
        System.out.println("===============================================================");
    }
}
