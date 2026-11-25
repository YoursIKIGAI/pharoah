package com.pharoah;

import com.pharoah.agents.impl.*;
import com.pharoah.ai.AIService;
import com.pharoah.ai.OfflineHeuristicAIService;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.knowledge.SubjectContextManager;
import com.pharoah.orchestrator.TaskOrchestrator;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.TaskDao;
import com.pharoah.tasks.TaskManager;

import java.io.File;
import java.util.List;

/**
 * Educational Demonstration and Test Suite for Milestones 11 and 12:
 * "Specialized Agent Swarm, Polymorphic Delegation, and Parallel Orchestration"
 */
public class AgentSwarmOrchestrationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("   👑 PHAROAH - MILESTONES 11 & 12: AGENT SWARM ORCHESTRATION  ");
        System.out.println("===============================================================\n");

        // 1. Setup Infrastructure
        new File("target/swarm_test.db").delete();
        DatabaseConnectionFactory dbFactory = new DatabaseConnectionFactory("jdbc:sqlite:target/swarm_test.db");
        TaskDao taskDao = new TaskDao(dbFactory);
        com.pharoah.persistence.dao.SubjectDao subjectDao = new com.pharoah.persistence.dao.SubjectDao(dbFactory);
        TaskManager taskManager = new TaskManager();
        ThreadPoolManager poolManager = new ThreadPoolManager(4);
        SubjectContextManager contextManager = new SubjectContextManager();

        TaskOrchestrator orchestrator = new TaskOrchestrator(taskManager, poolManager, contextManager, taskDao, subjectDao);

        // 2. Register all 10 specialized agents using Offline Heuristic AI Engine
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

        System.out.printf("\nSuccessfully registered %d specialized agents in swarm.\n\n",
                orchestrator.getRegisteredAgents().size());
        assert orchestrator.getRegisteredAgents().size() == 10 : "Expected 10 agents";

        // 3. Create diverse intellectual tasks
        Task t1 = new Task("MergeSort vs QuickSort", TaskType.COMPARISON, Priority.HIGH,
                "Time complexity in worst/average case, stability, auxiliary space.", "Data Structures");

        Task t2 = new Task("ACID Properties in Relational Databases", TaskType.STUDY_ASSISTANCE, Priority.HIGH,
                "Explain Atomicity, Consistency, Isolation, and Durability.", "DBMS");

        Task t3 = new Task("Zero-Knowledge Proofs in Cryptography", TaskType.RESEARCH, Priority.MEDIUM,
                "zk-SNARKs and verification overhead.", "Cybersecurity");

        Task t4 = new Task("BTech Capstone Project Roadmap", TaskType.PLANNING, Priority.MEDIUM,
                "3 months deadline, individual developer, Java architecture.", "Project");

        Task t5 = new Task("Dataset Analysis: Student Performance CSV", TaskType.DATA_ANALYSIS, Priority.LOW,
                "ID,Score,Grade\n1,95,A\n2,88,B\n3,92,A\n4,74,C\n5,99,A", "Academics");

        List<Task> batch = List.of(t1, t2, t3, t4, t5);

        // 4. Execute parallel batch via Orchestrator
        System.out.println("--- Executing Multi-Agent Parallel Batch Across Worker Threads ---");
        List<TaskResult> results = orchestrator.executeBatchParallel(batch);

        System.out.println("\n--- All Agent Results Collected & Verified ---");
        for (TaskResult r : results) {
            System.out.printf("  * Task #%s | Agent: %-20s | Duration: %d ms | Status: %s\n",
                    r.taskId().substring(0, 8), r.executedByAgent(), r.executionDurationMs(), r.status());
            assert r.status().name().equals("COMPLETED") : "Task failed";
        }

        // Verify specific agent routings
        assert results.get(0).executedByAgent().equals("ComparatorAgent") : "Expected ComparatorAgent for comparison";
        assert results.get(1).executedByAgent().equals("StudyAgent") : "Expected StudyAgent for study aid";
        assert results.get(2).executedByAgent().equals("ResearchAgent") : "Expected ResearchAgent for research";
        assert results.get(3).executedByAgent().equals("PlannerAgent") : "Expected PlannerAgent for planning";
        assert results.get(4).executedByAgent().equals("DataAnalystAgent") : "Expected DataAnalystAgent for data analysis";

        System.out.println("\n--- Inspecting Sample Output from ComparatorAgent ---");
        System.out.println(results.get(0).output());

        // Clean pool shutdown
        poolManager.gracefulShutdown(3);

        System.out.println("===============================================================");
        System.out.println("  🎉 MILESTONES 11 & 12 PASSED: MULTI-AGENT SWARM OPERATIONAL! ");
        System.out.println("===============================================================");
    }
}
