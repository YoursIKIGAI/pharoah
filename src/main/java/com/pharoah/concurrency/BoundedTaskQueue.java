package com.pharoah.concurrency;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A Thread-Safe Bounded Blocking Queue implementing the classic Producer-Consumer Pattern.
 * 
 * Demonstrates Core Java Inter-Thread Communication:
 * 1. Correct use of explicit intrinsic monitor locking (synchronized).
 * 2. Guarded suspension: wait() must ALWAYS be called within a while() loop to protect
 *    against spurious wakeups and race conditions.
 * 3. Correct monitor notification: notifyAll() wakes up waiting threads safely.
 * 4. Graceful shutdown coordination across producer and consumer threads.
 *
 * @param <T> The element type stored in the queue.
 */
public class BoundedTaskQueue<T> {

    private final Queue<T> queue;
    private final int capacity;
    private final Object monitorLock = new Object();
    private boolean isShutdown = false;

    /**
     * Initializes the bounded queue with a fixed maximum capacity.
     *
     * @param capacity The maximum number of elements the queue can hold simultaneously.
     */
    public BoundedTaskQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero.");
        }
        this.capacity = capacity;
        this.queue = new ArrayDeque<>(capacity);
    }

    /**
     * Producer Method: Enqueues an item into the queue.
     * If the queue is full, the calling thread WAITS until space becomes available.
     *
     * @param item The element to enqueue.
     * @throws InterruptedException if interrupted while waiting for space.
     * @throws IllegalStateException if the queue has been shut down.
     */
    public void enqueue(T item) throws InterruptedException {
        if (item == null) {
            throw new IllegalArgumentException("Cannot enqueue null item.");
        }

        synchronized (monitorLock) {
            // Guarded suspension pattern: while condition, NEVER if condition
            while (queue.size() == capacity && !isShutdown) {
                // Buffer is FULL -> Producer must wait
                monitorLock.wait();
            }

            if (isShutdown) {
                throw new IllegalStateException("Cannot enqueue: Queue has been shut down.");
            }

            queue.offer(item);

            // Notify waiting consumer threads that a new item is available
            monitorLock.notifyAll();
        }
    }

    /**
     * Consumer Method: Dequeues an item from the queue.
     * If the queue is empty, the calling thread WAITS until an item is enqueued.
     *
     * @return The dequeued item, or null if the queue is empty and shut down.
     * @throws InterruptedException if interrupted while waiting for an item.
     */
    public T dequeue() throws InterruptedException {
        synchronized (monitorLock) {
            // Guarded suspension pattern: Wait while queue is empty
            while (queue.isEmpty() && !isShutdown) {
                // Buffer is EMPTY -> Consumer must wait
                monitorLock.wait();
            }

            if (queue.isEmpty() && isShutdown) {
                return null; // Signals consumers that no more work will arrive
            }

            T item = queue.poll();

            // Notify waiting producer threads that space has freed up
            monitorLock.notifyAll();
            return item;
        }
    }

    /**
     * Returns the current number of elements in the queue.
     */
    public int size() {
        synchronized (monitorLock) {
            return queue.size();
        }
    }

    /**
     * Checks if the queue is empty.
     */
    public boolean isEmpty() {
        synchronized (monitorLock) {
            return queue.isEmpty();
        }
    }

    /**
     * Checks if the queue is at maximum capacity.
     */
    public boolean isFull() {
        synchronized (monitorLock) {
            return queue.size() == capacity;
        }
    }

    /**
     * Initiates graceful shutdown: unblocks all waiting threads and prohibits new items.
     */
    public void shutdown() {
        synchronized (monitorLock) {
            isShutdown = true;
            // Wake up all producers and consumers so they can notice the shutdown flag and exit
            monitorLock.notifyAll();
        }
    }

    public boolean isShutdown() {
        synchronized (monitorLock) {
            return isShutdown;
        }
    }
}
