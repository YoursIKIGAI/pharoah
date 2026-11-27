package com.pharoah.test;

import com.pharoah.concurrency.BoundedTaskQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Requirement 11(c): BoundedTaskQueue with 4 producers and 4 consumers loses/duplicates nothing.
 */
public class BoundedTaskQueueConcurrencyTest {

    @Test
    @DisplayName("4 Producers & 4 Consumers: Zero lost items, zero duplicate items")
    public void testBoundedQueueFourProducersFourConsumers() throws InterruptedException {
        final int QUEUE_CAPACITY = 8;
        final int PRODUCER_COUNT = 4;
        final int CONSUMER_COUNT = 4;
        final int ITEMS_PER_PRODUCER = 50;
        final int TOTAL_ITEMS = PRODUCER_COUNT * ITEMS_PER_PRODUCER; // 200 items total

        BoundedTaskQueue<Integer> queue = new BoundedTaskQueue<>(QUEUE_CAPACITY);
        Set<Integer> consumedSet = ConcurrentHashMap.newKeySet();
        List<Integer> consumedList = Collections.synchronizedList(new ArrayList<>());

        ExecutorService producerPool = Executors.newFixedThreadPool(PRODUCER_COUNT);
        ExecutorService consumerPool = Executors.newFixedThreadPool(CONSUMER_COUNT);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch producersDone = new CountDownLatch(PRODUCER_COUNT);
        CountDownLatch consumersDone = new CountDownLatch(CONSUMER_COUNT);

        // Start 4 Consumer threads
        for (int c = 0; c < CONSUMER_COUNT; c++) {
            consumerPool.submit(() -> {
                try {
                    startLatch.await();
                    while (true) {
                        Integer item = queue.dequeue();
                        if (item == null) {
                            // Queue was shut down and is now empty
                            break;
                        }
                        consumedSet.add(item);
                        consumedList.add(item);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumersDone.countDown();
                }
            });
        }

        // Start 4 Producer threads
        for (int p = 0; p < PRODUCER_COUNT; p++) {
            final int producerId = p;
            producerPool.submit(() -> {
                try {
                    startLatch.await();
                    int startVal = producerId * ITEMS_PER_PRODUCER;
                    for (int i = 0; i < ITEMS_PER_PRODUCER; i++) {
                        int val = startVal + i;
                        queue.enqueue(val);
                        // Brief pause to induce contention
                        if (i % 10 == 0) Thread.sleep(2);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                }
            });
        }

        // Release the latch so all 8 threads start concurrently
        startLatch.countDown();

        // Wait for all producers to finish enqueuing their items
        boolean producersFinished = producersDone.await(10, TimeUnit.SECONDS);
        assertTrue(producersFinished, "All producers should finish within timeout");

        // Gracefully shutdown the queue so consumers can drain remaining items and terminate
        queue.shutdown();

        // Wait for all consumers to finish draining the queue
        boolean consumersFinished = consumersDone.await(10, TimeUnit.SECONDS);
        assertTrue(consumersFinished, "All consumers should finish draining within timeout");

        producerPool.shutdown();
        consumerPool.shutdown();

        // Verification: Exactly 200 items consumed, zero lost, zero duplicates
        assertEquals(TOTAL_ITEMS, consumedList.size(), "Total consumed items must equal total produced items (no items lost)");
        assertEquals(TOTAL_ITEMS, consumedSet.size(), "Distinct consumed items must equal total produced items (no duplicates)");

        // Verify every integer from 0 to 199 is accounted for
        for (int i = 0; i < TOTAL_ITEMS; i++) {
            assertTrue(consumedSet.contains(i), "Item " + i + " was lost!");
        }
    }
}
