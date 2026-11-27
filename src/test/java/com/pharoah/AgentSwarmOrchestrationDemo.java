package com.pharoah;

import com.pharoah.agents.AgentConfig;
import com.pharoah.agents.AgentRole;
import com.pharoah.agents.AgentService;
import com.pharoah.ai.AIService;
import com.pharoah.ai.OfflineHeuristicAIService;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.orchestrator.TaskOrchestrator;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.AgentDao;
import com.pharoah.persistence.dao.TaskDao;
import com.pharoah.tasks.TaskManager;

import java.util.List;

// VARIJ: Educational test suite for multi-agent swarm orchestration and concurrent batch execution.
public class AgentSwarmOrchestrationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("   👑 PHAROAH - AGENT SWARM ORCHESTRATION DEMO                ");
        System.out.println("===============================================================\n");

        DatabaseConnectionFactory dbFactory = new DatabaseConnectionFactory();
        TaskDao taskDao = new TaskDao(dbFactory);
        AgentDao agentDao = new AgentDao(dbFactory);
        AgentService agentService = new AgentService(agentDao);
        TaskManager taskManager = new TaskManager();
        ThreadPoolManager poolManager = new ThreadPoolManager(4);
        AIService aiEngine = new OfflineHeuristicAIService();

        TaskOrchestrator orchestrator = new TaskOrchestrator(taskManager, poolManager, taskDao, agentService, aiEngine);

        List<AgentConfig> agents = agentService.listAllAgents();
        System.out.printf("Loaded %d registered agents from database:\n", agents.size());
        for (AgentConfig a : agents) {
            System.out.println("  * " + a);
        }

        // Create tasks
        Task t1 = new Task("MergeSort vs QuickSort", TaskType.COMPARISON, Priority.HIGH,
                "Time complexity in worst/average case, stability, auxiliary space.", "proj-default-01");
        Task t2 = new Task("Two-Phase Locking (2PL)", TaskType.STUDY_ASSISTANCE, Priority.HIGH,
                "Growing and shrinking phase serialization.", "proj-default-01");
        Task t3 = new Task("Virtual Memory Paging", TaskType.RESEARCH, Priority.MEDIUM,
                "TLB hit rate and page fault handling.", "proj-default-01");

        List<Task> batch = List.of(t1, t2, t3);
        System.out.println("\nExecuting batch concurrently across worker threads...");
        List<TaskResult> results = orchestrator.executeBatch(batch);

        System.out.println("\nResults Received:");
        for (TaskResult r : results) {
            System.out.printf("  * Task #%s | Agent: %-22s | Duration: %d ms | Status: %s\n",
                    r.taskId().substring(0, 8), r.executedByAgent(), r.executionDurationMs(), r.status());
            assert r.status().name().equals("COMPLETED") : "Task should complete";
        }

        poolManager.gracefulShutdown(2);
        System.out.println("\n🎉 AGENT SWARM DEMO COMPLETED SUCCESSFULLY!");
    }
}
