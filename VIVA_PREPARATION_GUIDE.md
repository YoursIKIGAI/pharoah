# 👑 PHAROAH: Academic Viva & Defense Preparation Guide

**Author:** Varij Chauhan (BTech CSE)  
**Project:** PHAROAH – Java-Based Multi-Agent Task Intelligence & Knowledge Management System  
**Audience:** External University Examiners, Professors, and Evaluation Panel

---

## 🎯 Top 10 High-Probability Viva Questions & Master Answers

### Q1: Why did you use `PriorityQueue` instead of a regular `Queue` or `ArrayList`?
> **Answer:**  
> In an intellectual multi-agent system, tasks have different urgency levels (HIGH, MEDIUM, LOW). A regular `Queue` or `ArrayList` processes items strictly First-In-First-Out (FIFO) or requires manual $O(N \log N)$ re-sorting every time a task is added.  
> `PriorityQueue` is backed by a **Binary Heap**. It maintains partial order in $O(\log N)$ insertion time and guarantees that `.poll()` always extracts the highest-priority task in $O(\log N)$ time. In our `Task.java`, we implemented the `Comparable<Task>` interface, which compares priority levels first and uses creation timestamps as a FIFO tie-breaker.

---

### Q2: In your `BoundedTaskQueue`, why did you put `wait()` inside a `while` loop instead of an `if` block?
> **Answer:**  
> Putting `wait()` inside a `while` loop is the **Guarded Suspension Pattern**. There are two critical reasons:
> 1. **Spurious Wakeups:** The operating system or JVM can occasionally wake up a waiting thread even if no `notify()` was called. If we used `if`, the thread would proceed without re-checking the condition, causing a buffer overflow or underflow.
> 2. **State Changes Under Competition:** When multiple consumer threads wake up simultaneously via `notifyAll()`, one consumer might consume the item before the second consumer gets CPU time. The `while` loop forces the second thread to re-verify `queue.isEmpty()` and go back to waiting safely.

---

### Q3: What is the difference between `Runnable` and `Callable` in your concurrency engine?
> **Answer:**  
> * `Runnable.run()` returns `void` and cannot throw checked exceptions. It is suitable for fire-and-forget background threads (like our `WorkerThread` or heartbeat timer).
> * `Callable.call()` returns a parameterized value `V` and can declare checked exceptions. In `AgentExecutionTask`, we implemented `Callable<TaskResult>` so the orchestrator can submit work to an `ExecutorService` and retrieve the computed result asynchronously via `Future<TaskResult>.get()`.

---

### Q4: How did you implement Transactions in JDBC, and how do you guarantee atomic rollback?
> **Answer:**  
> By default, JDBC connections have `autoCommit = true`. In our `TransactionManager`:
> 1. We disable autocommit using `connection.setAutoCommit(false)`.
> 2. We execute multiple statements across tables (e.g. inserting into `subjects` and `tasks`).
> 3. If all statements succeed, we call `connection.commit()`.
> 4. If any `SQLException` or `RuntimeException` is thrown, the catch block calls `connection.rollback()`, ensuring no partial or corrupted records persist in the database.
> 5. In the `finally` block, we restore `setAutoCommit(true)` and close the connection.

---

### Q5: Why did you use `PreparedStatement` instead of `Statement`?
> **Answer:**  
> 1. **Security (SQL Injection Prevention):** `PreparedStatement` uses parameter placeholders (`?`). The database driver compiles the SQL structure first and treats user parameters strictly as literal data, preventing malicious SQL injection attacks.
> 2. **Performance (Pre-compilation):** If the query is executed repeatedly, the database query optimizer parses and compiles the query plan once, reducing overhead.
> 3. **Type Safety:** It provides typed setter methods like `setString()`, `setTimestamp()`, and `setInt()` instead of fragile manual string concatenations.

---

### Q6: What are Java Records and why did you use `TaskResult` as a record?
> **Answer:**  
> Records were finalized in modern Java as concise, immutable data-carrier classes. When we declare:
> ```java
> public record TaskResult(String taskId, TaskStatus status, String output, long executionDurationMs, String executedByAgent, LocalDateTime finishedAt) {}
> ```
> Java automatically generates `final` fields, a canonical constructor, accessor methods (e.g. `result.status()`), an invariant-respecting `equals()`, `hashCode()`, and a readable `toString()`. This eliminates boilerplate while guaranteeing immutability across concurrent threads.

---

### Q7: Explain PECS and how Bounded Wildcards are used in your Generic Repository.
> **Answer:**  
> **PECS** stands for **Producer Extends, Consumer Super**:
> * In `InMemoryGenericRepository.java`, the method `saveAll(Collection<? extends T> entities)` uses an upper-bounded wildcard (`? extends T`). Here, the collection is a *Producer* supplying items to be read and saved. This allows us to pass a `List<SubTask>` to a repository handling `Task`.
> * In `mapAll(Function<? super T, ? extends R> mapper)`, `? super T` is used because the mapper *Consumes* `T` (Contravariance), and `? extends R` because it *Produces* `R` (Covariance).

---

### Q8: What Stream Collectors did you implement in `TaskAnalyticsEngine`?
> **Answer:**  
> We utilized:
> 1. `Collectors.groupingBy(Task::getType, Collectors.counting())`: Groups tasks by intellectual type and counts their occurrences.
> 2. `Collectors.partitioningBy(TaskAnalyticsEngine.IS_COMPLETED)`: Partitions tasks into two distinct boolean groups (`true` = completed, `false` = in-progress/failed).
> 3. `Collectors.joining(", ")`: Concatenates distinct subject names into a formatted string.
> 4. `Collectors.summarizingDouble(t -> t.getInputPayload().length())`: Computes summary statistics (count, sum, min, max, average) in a single pass over the stream.

---

### Q9: How does PHAROAH prevent context leakage between different subjects?
> **Answer:**  
> Through our `SubjectContextManager`. Each subject has an independent `SubjectMemory` instance. When an agent executes, the Orchestrator supplies *only* the `SubjectMemory` linked to the task's `subjectId`. Furthermore, `validateIsolation()` checks that if workspace "Java" is active, it cannot access notes or conversation history belonging to "DBMS" unless explicit cross-subject permission is granted by the user.

---

### Q10: What is your project's fallback strategy if external AI APIs fail or internet drops?
> **Answer:**  
> PHAROAH uses a **Dual-Engine SPI architecture**:
> 1. `OfflineHeuristicAIService` is written in 100% pure Java. It uses term-frequency scoring for extractive summarization, cloze-deletion generation for MCQs, and tabular synthesis for comparisons. It runs completely offline with zero dependencies and zero API keys.
> 2. `RemoteHttpAIService` connects to cloud LLM APIs (Gemini/OpenAI) using Java 21's `HttpClient`. If the network drops or an API key is missing, it automatically and gracefully delegates to the offline engine without throwing uncaught exceptions.
