package com.pharoah.concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central Thread Pool and Scheduler Manager for PHAROAH.
 * 
 * Demonstrates Java Executor Framework concepts:
 * 1. ExecutorService with custom thread factory for thread naming.
 * 2. ScheduledExecutorService for periodic background operations.
 * 3. Callable<T> submission and Future<T> result handling.
 * 4. Two-phase graceful shutdown pattern (shutdown() -> awaitTermination() -> shutdownNow()).
 */
public class ThreadPoolManager {

    private final ExecutorService workerPool;
    private final ScheduledExecutorService scheduledPool;
    private final int poolSize;

    /**
     * Shared ThreadFactory creator for worker threads with customizable prefix.
     */
    public static ThreadFactory createWorkerThreadFactory(String prefix) {
        return new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, prefix + "-" + count.getAndIncrement());
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
    }

    /**
     * Initializes the thread pools with a specified number of worker threads.
     *
     * @param workerCount Number of worker threads (e.g. Runtime.getRuntime().availableProcessors()).
     */
    public ThreadPoolManager(int workerCount) {
        this.poolSize = Math.max(2, workerCount);

        ThreadFactory workerFactory = createWorkerThreadFactory("PHAROAH-WorkerPool");

        ThreadFactory schedulerFactory = new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "PHAROAH-Scheduler-" + count++);
                t.setDaemon(true); // Daemon so it does not block JVM exit
                return t;
            }
        };

        this.workerPool = Executors.newFixedThreadPool(this.poolSize, workerFactory);
        this.scheduledPool = Executors.newScheduledThreadPool(2, schedulerFactory);
    }

    public ThreadPoolManager() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Submits a Callable task to the worker pool and returns a Future.
     */
    public <T> Future<T> submitTask(Callable<T> task) {
        if (workerPool.isShutdown()) {
            throw new IllegalStateException("Cannot submit: Thread pool is already shut down.");
        }
        return workerPool.submit(task);
    }

    /**
     * Submits a Runnable task to the worker pool.
     */
    public Future<?> submitTask(Runnable task) {
        if (workerPool.isShutdown()) {
            throw new IllegalStateException("Cannot submit: Thread pool is already shut down.");
        }
        return workerPool.submit(task);
    }

    /**
     * Schedules a recurring periodic maintenance task (e.g. heartbeat, auto-save).
     *
     * @param command      The task to run.
     * @param initialDelay Initial delay before first run.
     * @param period       Period between successive runs.
     * @param unit         Time unit.
     * @return ScheduledFuture representing the repeating task.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduledPool.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * Executes the standard two-phase graceful shutdown pattern recommended by Oracle.
     *
     * @param timeoutSeconds Maximum seconds to wait for running tasks to complete.
     */
    public void gracefulShutdown(long timeoutSeconds) {
        System.out.println("[POOL SHUTDOWN] Initiating graceful shutdown of thread pools...");

        workerPool.shutdown();     // Disable new tasks from being submitted
        scheduledPool.shutdown();  // Disable new scheduled tasks

        try {
            // Wait for existing tasks to terminate
            if (!workerPool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                System.out.println("[POOL SHUTDOWN] Pool did not terminate in time. Forcing cancellation...");
                workerPool.shutdownNow(); // Cancel currently executing tasks
                scheduledPool.shutdownNow();

                if (!workerPool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    System.err.println("[POOL SHUTDOWN] Worker pool did not terminate cleanly.");
                }
            } else {
                System.out.println("[POOL SHUTDOWN] All worker threads terminated cleanly.");
            }
        } catch (InterruptedException ie) {
            workerPool.shutdownNow();
            scheduledPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getPoolSize() {
        return poolSize;
    }

    public boolean isShutdown() {
        return workerPool.isShutdown();
    }
}
