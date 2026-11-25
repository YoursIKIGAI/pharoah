# 👑 PHAROAH: Java-Based Multi-Agent Task Intelligence & Knowledge Management System

**Developer:** Varij Chauhan (BTech CSE)  
**Academic Mentor / Architect:** Antigravity AI  
**Platform:** Java 21 LTS  
**Persistence:** SQLite JDBC (Portably self-contained in `pharoah.db`)  
**Build System:** Maven Compatible (`pom.xml` provided)

---

## 📖 Project Overview

**PHAROAH** is an intelligent, multi-agent task execution and knowledge management platform designed for university students, researchers, and intellectual knowledge workers. 

It solves the problem of **context fragmentation**—where students repeatedly lose continuity when switching between diverse computer science subjects (such as *Java*, *Operating Systems*, and *DBMS*). PHAROAH maintains **isolated subject knowledge spaces** with independent memory and documents, while deploying a concurrent **swarm of 10 specialized agents** to perform research, paper reading, writing, comparisons, fact-checking, data analysis, study guide generation, and project planning.

Simultaneously, PHAROAH serves as a comprehensive academic showcase of Core and Advanced Java syllabus concepts:
* **Object-Oriented Programming:** Encapsulation, Polymorphism, Abstract classes, Records, Enums, `Comparable`.
* **Collections Framework:** `ArrayList`, `HashSet`, `HashMap`, `PriorityQueue` (Heap scheduling), `ConcurrentHashMap`.
* **Multithreading Basics:** Dedicated `WorkerThread`, Thread lifecycle states (`NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`), priorities, daemon threads, `join()`, `interrupt()`.
* **Inter-Thread Communication:** Custom `BoundedTaskQueue<T>` with guarded suspension (`while (isFull()) wait()`) and `notifyAll()` monitor synchronization.
* **Executor Framework:** `ThreadPoolManager`, `ExecutorService`, `ScheduledExecutorService`, `Callable<TaskResult>`, `Future.get()`.
* **JDBC & Database Architecture:** `DriverManager`, `Connection`, `PreparedStatement` (SQL injection prevention), `ResultSet`, `ResultSetMetaData`, and atomic `commit()`/`rollback()` transactions.
* **Lambdas & Functional Interfaces:** `Predicate`, `Function`, `Consumer`, `Supplier`, and Method References.
* **Stream API & Analytics:** `filter`, `map`, `sorted`, `distinct`, `groupingBy`, `partitioningBy`, `joining`, `summarizingDouble`, `reduce`.
* **Generics & Bounded Types:** Reusable `Repository<T, ID>` with PECS wildcards (`<? extends T>`, `<? super T>`).
* **File Handling:** `BufferedReader`, `BufferedWriter`, `FileReader`, `FileWriter`, `FileInputStream`, `FileOutputStream`, and `StreamTokenizer`.
* **Offline Dual AI Strategy:** 100% offline heuristic rule engine for resilient academic evaluation + pluggable `java.net.http.HttpClient` cloud LLM connector.

---

## 🏛️ System Architecture

```text
com.pharoah
├── app
│   └── PharoahApplication.java        // Main interactive console dashboard
├── core
│   ├── enums                          // Priority, TaskType, TaskStatus
│   └── model                          // Task, TaskResult (Record), Subject
├── tasks
│   └── TaskManager.java               // Collections coordinator (Heap PriorityQueue, HashMap, etc.)
├── concurrency
│   ├── WorkerThread.java              // Custom Thread with lifecycle tracking
│   ├── BoundedTaskQueue.java          // Producer-Consumer with wait()/notifyAll()
│   ├── AgentExecutionTask.java        // Callable<TaskResult>
│   └── ThreadPoolManager.java         // ExecutorService & ScheduledExecutorService
├── persistence
│   ├── DatabaseConnectionFactory.java // SQLite JDBC driver manager & DDL bootstrap
│   ├── TransactionManager.java        // Atomic commit() & rollback()
│   ├── repository                     // Generic Repository<T, ID> & Bounded Wildcards
│   └── dao                            // TaskDao & SubjectDao with PreparedStatement
├── analytics
│   └── TaskAnalyticsEngine.java       // Stream API & Collectors analytics engine
├── documents
│   └── DocumentFileProcessor.java     // Byte/Char streams, CSV export, StreamTokenizer
├── knowledge
│   ├── SubjectMemory.java             // Subject-specific context memory
│   └── SubjectContextManager.java     // Workspace boundary isolation guard
├── ai
│   ├── AIService.java                 // Modular intelligence abstraction
│   ├── OfflineHeuristicAIService.java // 100% pure Java offline TF-IDF heuristic engine
│   └── RemoteHttpAIService.java       // Java 21 HttpClient cloud API connector
├── agents
│   ├── Agent.java                     // Core agent polymorphic interface
│   ├── AbstractAgent.java             // Template Method base agent
│   └── impl                           // 10 Specialized Swarm Agents:
│       ├── WriterAgent.java
│       ├── ResearchAgent.java
│       ├── PaperReaderAgent.java
│       ├── SummariserAgent.java
│       ├── ComparatorAgent.java
│       ├── VerificationAgent.java
│       ├── DataAnalystAgent.java
│       ├── StudyAgent.java
│       ├── PlannerAgent.java
│       └── DocumentAnalystAgent.java
└── orchestrator
    └── TaskOrchestrator.java          // Swarm routing, parallel delegation, state sync
```

---

## 🚀 How to Run the Application

### 1. Interactive Console Mode
Open your terminal (PowerShell, Command Prompt, or VS Code terminal):
```powershell
cd "c:\Users\varij chauhan\OneDrive\Desktop\pharoah"
java -cp "target\classes;lib\*" com.pharoah.app.PharoahApplication
```

### 2. Automated Demonstration Mode (For Quick Viva Demos)
Runs a complete end-to-end task execution, agent swarm delegation, stream analytics, and disk export:
```powershell
cd "c:\Users\varij chauhan\OneDrive\Desktop\pharoah"
java -cp "target\classes;lib\*" com.pharoah.app.PharoahApplication --demo
```

---

## 🧪 Independent Verification Test Suites

Every major concept has its own dedicated educational test suite:

1. **OOP & Collections (Milestone 1):**
   ```powershell
   java -cp "target\classes;lib\*" -ea com.pharoah.Milestone1Demo
   ```
2. **Multithreading, wait/notify, Executors (Milestones 2, 3, 4):**
   ```powershell
   java -cp "target\classes;lib\*" -ea com.pharoah.ConcurrencyDemo
   ```
3. **JDBC, PreparedStatement, Metadata, Transactions (Milestones 5, 6):**
   ```powershell
   java -cp "target\classes;lib\*" -ea com.pharoah.JdbcPersistenceDemo
   ```
4. **Lambdas, Functional Interfaces & Streams (Milestones 7, 8):**
   ```powershell
   java -cp "target\classes;lib\*" -ea com.pharoah.FunctionalStreamsDemo
   ```
5. **Generics, File I/O & Knowledge Spaces (Milestones 9, 10):**
   ```powershell
   java -cp "target\classes;lib\*" -ea com.pharoah.GenericsFilesKnowledgeDemo
   ```
6. **Agent Swarm & Orchestration (Milestones 11, 12):**
   ```powershell
   java -cp "target\classes;lib\*" -ea com.pharoah.AgentSwarmOrchestrationDemo
   ```
