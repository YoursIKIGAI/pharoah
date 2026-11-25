package com.pharoah.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Worker Thread for PHAROAH.
 * 
 * Demonstrates Core Java Multithreading concepts:
 * 1. Extending Thread class and overriding run().
 * 2. Thread Naming & ID management.
 * 3. Thread Priorities (MIN_PRIORITY, NORM_PRIORITY, MAX_PRIORITY).
 * 4. Daemon vs User thread modes.
 * 5. Cooperative Cancellation via interrupt() and InterruptedException handling.
 * 6. Thread States: NEW, RUNNABLE, TIMED_WAITING, TERMINATED.
 */
public class WorkerThread extends Thread {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private final Runnable taskPayload;
    private final boolean isDaemonWorker;

    /**
     * Constructs a managed Worker Thread.
     *
     * @param namePrefix     Human-readable name identifier.
     * @param taskPayload    The Runnable code to execute.
     * @param isDaemonWorker Whether this thread should run in daemon mode (background).
     * @param priority       Thread priority level (1 to 10).
     */
    public WorkerThread(String namePrefix, Runnable taskPayload, boolean isDaemonWorker, int priority) {
        super(namePrefix + "-Worker-" + THREAD_COUNTER.getAndIncrement());
        this.taskPayload = taskPayload;
        this.isDaemonWorker = isDaemonWorker;

        setDaemon(isDaemonWorker);
        setPriority(Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, priority)));
    }

    public WorkerThread(String namePrefix, Runnable taskPayload) {
        this(namePrefix, taskPayload, false, Thread.NORM_PRIORITY);
    }

    @Override
    public void run() {
        System.out.printf("[THREAD START] %s | Priority: %d | Daemon: %b | State: %s\n",
                getName(), getPriority(), isDaemon(), getState());

        try {
            if (taskPayload != null) {
                taskPayload.run();
            }
        } catch (Exception e) {
            System.err.printf("[THREAD ERROR] Uncaught exception in %s: %s\n", getName(), e.getMessage());
        } finally {
            System.out.printf("[THREAD END] %s has completed execution.\n", getName());
        }
    }

    /**
     * Utility method to simulate work and demonstrate sleep and interrupt handling.
     *
     * @param durationMs Milliseconds to sleep.
     * @throws InterruptedException if interrupted while sleeping.
     */
    public static void safeSleep(long durationMs) throws InterruptedException {
        Thread.sleep(durationMs);
    }
}
