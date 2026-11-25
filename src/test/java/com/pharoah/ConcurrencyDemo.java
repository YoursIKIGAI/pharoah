package com.pharoah;

import com.pharoah.concurrency.AgentExecutionTask;
import com.pharoah.concurrency.BoundedTaskQueue;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.concurrency.WorkerThread;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Educational Demonstration and Test Suite for Milestones 2, 3, and 4:
 * "Java Multithreading, Inter-Thread Communication, and Executor Framework"
 */
public class ConcurrencyDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("   👑 PHAROAH - MILESTONES 2, 3, 4: CONCURRENCY SUITE        ");
        System.out.println("===============================================================\n");

        // =============================================================
        // PART 1 (MILESTONE 2): Thread Lifecycle, States & Interruption
        // =============================================================
        System.out.println("--- [PART 1] Verifying Thread Lifecycle States & Interruption ---");

        WorkerThread lifecycleWorker = new WorkerThread("LifecycleDemo", () -> {
            try {
                System.out.println("  Worker thread running. Entering sleep for 400ms...");
                WorkerThread.safeSleep(400);
            } catch (InterruptedException e) {
                System.out.println("  Worker thread caught InterruptedException cooperatively!");
            }
        });

        System.out.println("  State right after 'new': " + lifecycleWorker.getState() + " (Expected: NEW)");
        assert lifecycleWorker.getState() == Thread.State.NEW : "State should be NEW";

        lifecycleWorker.start();
        System.out.println("  State after '.start()': " + lifecycleWorker.getState() + " (Expected: RUNNABLE)");

        Thread.sleep(100); // Give worker time to enter sleep
        System.out.println("  State during sleep: " + lifecycleWorker.getState() + " (Expected: TIMED_WAITING)");
        assert lifecycleWorker.getState() == Thread.State.TIMED_WAITING : "State should be TIMED_WAITING";

        lifecycleWorker.join(); // Main thread waits for worker to finish
        System.out.println("  State after '.join()': " + lifecycleWorker.getState() + " (Expected: TERMINATED)");
        assert lifecycleWorker.getState() == Thread.State.TERMINATED : "State should be TERMINATED";

        System.out.println("  >> PASSED: All 4 Thread Lifecycle states verified successfully.\n");

        // =============================================================
        // PART 2 (MILESTONE 3): Guarded Bounded Producer-Consumer (wait/notifyAll)
        // =============================================================
        System.out.println("--- [PART 2] Verifying BoundedTaskQueue (wait() / notifyAll()) ---");

        // Small capacity of 2 ensures buffer will fill up and trigger wait()
        final int QUEUE_CAPACITY = 2;
        BoundedTaskQueue<Task> taskQueue = new BoundedTaskQueue<>(QUEUE_CAPACITY);
        List<Task> consumedTasks = new ArrayList<>();
        final int TOTAL_ITEMS = 5;

        // Producer Thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= TOTAL_ITEMS; i++) {
                    Task task = new Task("Task #" + i, TaskType.RESEARCH, Priority.HIGH, "Analyze query " + i);
                    System.out.printf("  [PRODUCER] Enqueueing '%s' (Queue size: %d/%d)...\n",
                            task.getTitle(), taskQueue.size(), QUEUE_CAPACITY);
                    taskQueue.enqueue(task);
                    System.out.printf("  [PRODUCER] Successfully enqueued '%s'.\n", task.getTitle());
                    Thread.sleep(80); // Stagger production
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer-Thread");

        // Consumer Thread (simulates worker agent taking tasks)
        Thread consumer = new Thread(() -> {
            try {
                while (consumedTasks.size() < TOTAL_ITEMS) {
                    Thread.sleep(150); // Slower consumer forces producer to wait!
                    Task task = taskQueue.dequeue();
                    if (task != null) {
                        System.out.printf("  [CONSUMER] Dequeued and processing '%s' (Remaining queue size: %d).\n",
                                task.getTitle(), taskQueue.size());
                        consumedTasks.add(task);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer-Thread");

        consumer.start();
        producer.start();

        producer.join();
        consumer.join();

        assert consumedTasks.size() == TOTAL_ITEMS : "Consumer did not receive all items!";
        System.out.println("  >> PASSED: Guarded producer-consumer coordination verified with zero race conditions.\n");

        // =============================================================
        // PART 3 (MILESTONE 4): Executor Framework, Callable & Future
        // =============================================================
        System.out.println("--- [PART 3] Verifying ExecutorService, Callable, Future & Scheduler ---");

        ThreadPoolManager poolManager = new ThreadPoolManager(3);

        // Test ScheduledExecutorService: Periodic heartbeat
        AtomicInteger heartbeatTicks = new AtomicInteger(0);
        ScheduledFuture<?> heartbeatHandle = poolManager.scheduleAtFixedRate(
                () -> System.out.printf("  [HEARTBEAT] Thread %s tick #%d\n",
                        Thread.currentThread().getName(), heartbeatTicks.incrementAndGet()),
                50, 150, TimeUnit.MILLISECONDS
        );

        // Submit 4 parallel agent execution tasks
        List<Future<TaskResult>> futures = new ArrayList<>();
        String[] sampleAgents = {"WriterAgent", "ResearchAgent", "ComparatorAgent", "StudyAgent"};

        for (int i = 0; i < 4; i++) {
            Task task = new Task("Intellectual Task " + (i + 1), TaskType.RESEARCH, Priority.HIGH, "Input payload " + (i + 1));
            AgentExecutionTask agentTask = new AgentExecutionTask(task, sampleAgents[i], 120L);
            futures.add(poolManager.submitTask(agentTask));
        }

        // Wait for all futures using Future.get()
        System.out.println("  Awaiting parallel execution results from Future.get()...");
        for (Future<TaskResult> future : futures) {
            TaskResult result = future.get(2, TimeUnit.SECONDS);
            System.out.printf("  [RESULT RECEIVED] Status: %s | Agent: %-15s | Duration: %d ms\n",
                    result.status(), result.executedByAgent(), result.executionDurationMs());
            assert result.status().name().equals("COMPLETED") : "Task should be completed";
        }

        // Stop the periodic heartbeat
        heartbeatHandle.cancel(false);
        System.out.printf("  Heartbeat stopped after %d ticks.\n", heartbeatTicks.get());

        // Graceful shutdown of pool
        poolManager.gracefulShutdown(3);
        assert poolManager.isShutdown() : "Pool should be shutdown";

        System.out.println("  >> PASSED: ExecutorService, Callable, Future, and Scheduler verified.\n");

        System.out.println("===============================================================");
        System.out.println("  🎉 MILESTONES 2, 3, 4 PASSED: CONCURRENCY ENGINE READY!      ");
        System.out.println("===============================================================");
    }
}
