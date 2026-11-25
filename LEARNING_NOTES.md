# 🎓 PHAROAH: Student Learning Notes & Course Concepts

These notes explain the key Java concepts implemented in PHAROAH in simple, clear English for our viva defense and exams.

---

## 1. Object-Oriented Programming (OOP)

* **Encapsulation:** Keeping fields `private` inside entities (`Task`, `Project`, `AgentConfig`) and exposing controlled access via public getters and setters with validation rules.
* **Polymorphism:** The `TaskOrchestrator` interacts with agents via standard abstractions (`Agent` interface or `AgentConfig` roles) rather than being tied to specific agent implementations.
* **Template Method Pattern:** In `AbstractAgent.java`, the `execute()` method handles common timing, error containment, and logging, while subclasses or AI services supply the specific `doExecute()` intellectual logic.

---

## 2. Collections Framework

* **`PriorityQueue<Task>`:** Uses a **Min/Max Binary Heap**. When tasks are enqueued, Java orders them using our `Comparable<Task>` implementation. Tasks with `Priority.HIGH` are extracted first in $O(\log N)$ time when calling `.poll()`.
* **`HashMap<String, Task>`:** Provides $O(1)$ constant-time lookup by unique ID (`taskId`).
* **`HashSet<String>`:** Guarantees unique entity IDs and prevents duplicate registrations in $O(1)$ time.
* **`ConcurrentHashMap<String, Task>`:** Thread-safe hash table that allows simultaneous reads and writes from multiple worker threads without locking the entire map.

---

## 3. Multithreading & Inter-Thread Communication

* **Thread Lifecycle States:** A Java thread progresses through `NEW` $\to$ `RUNNABLE` $\to$ `TIMED_WAITING` $\to$ `TERMINATED`.
* **Guarded Suspension Pattern (`wait()` & `notifyAll()`):** In `BoundedTaskQueue.java`, threads call `wait()` when the queue is full (producers) or empty (consumers). `wait()` is placed inside a `while` loop to guard against **spurious wakeups** and race conditions.
* **Daemon Threads:** Set using `thread.setDaemon(true)`. Daemon threads run background maintenance tasks (like scheduled heartbeats) without preventing the JVM from shutting down.

---

## 4. The Executor Framework

* **`ExecutorService`:** Manages a thread pool (`Executors.newFixedThreadPool(N)`). Reuses existing threads rather than repeatedly incurring the overhead of creating new OS-level threads.
* **`Callable<V>` vs `Runnable`:** `Callable.call()` can return a computed result and throw checked exceptions, whereas `Runnable.run()` returns `void`. In PHAROAH, `AgentExecutionTask` implements `Callable<TaskResult>`.
* **`Future<V>`:** A handle to an asynchronous computation. Calling `future.get()` blocks until the worker thread finishes and returns the result.

---

## 5. JDBC & Relational Persistence (MySQL)

* **`PreparedStatement`:** Uses SQL placeholders (`?`). Pre-compiles the query and binds parameters safely, completely preventing **SQL Injection Attacks**.
* **Transactions (`commit()` and `rollback()`):** By calling `conn.setAutoCommit(false)`, multiple insert/update statements are treated as a single atomic unit. If an error occurs, `conn.rollback()` undoes all uncommitted changes, preserving database consistency.
* **`ResultSet` & Object-Relational Mapping (ORM):** We manually map SQL table rows back into Java domain objects (`Project`, `Task`, `DocumentSection`) using typed getters (`getString`, `getTimestamp`, `getInt`).

---

## 6. Functional Programming & Stream API

* **Functional Interfaces:** `Predicate<T>` (boolean condition), `Function<T, R>` (transformation), `Consumer<T>` (side effect), and `Supplier<T>` (factory).
* **Stream Pipeline:** Streams process collections declaratively without mutating the underlying data.
  * `filter(Predicate)`: Filters matching elements.
  * `map(Function)`: Transforms elements.
  * `Collectors.groupingBy(...)`: Groups tasks by category into a map of counts.
  * `Collectors.partitioningBy(...)`: Splits tasks into two lists based on a boolean condition (e.g. Completed vs Incomplete).
  * `Collectors.summarizingDouble(...)`: Computes summary statistics (count, sum, min, max, average) in a single pass.
