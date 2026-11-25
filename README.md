# 👑 PHAROAH: Terminal-Based Multi-Agent Orchestration & Knowledge Management System

**Platform:** Java 21 LTS  
**Persistence:** MySQL 8.0 (JDBC) with automatic SQLite fallback  
**Interface:** Pure Terminal Command-Line Application (No web server, no GUI)  
**Developers:** Varij Chauhan, Ashutosh, Sanchit (BTech CSE)

---

## 📖 1. Project Overview

**PHAROAH** is a terminal-based multi-agent task execution and knowledge management platform designed for university students, researchers, and technical professionals.

It acts as a centralized command-line workspace where users can:
1. **Manage Multiple Projects:** Create, switch, and maintain isolated project workspaces (*Research Papers, Technical Blogs, Documentation, Coding Projects*).
2. **Task Decomposition & Dependency Graphs:** Decompose complex goals into dependent subtasks scheduled by priority.
3. **Configurable Swarm Agents:** Register, configure, and assign 7 specialized agent roles (*Research, Writing, Analysis, Coding, Documentation, Reviewer, Planning*).
4. **Human-in-the-Loop Orchestration:** Execute tasks concurrently using Java thread pools, while gating final completion behind human review and approval.
5. **Persistent Knowledge Repository:** Store, categorize, and search URLs, research papers, notes, code snippets, and citations.
6. **Document Workflow Management:** Draft, review, and compile multi-section documents (*Academic Research Papers, Technical Blogs, Technical Documentation, Business Proposals*).
7. **Relational MySQL Persistence:** Persist all records via standard JDBC `PreparedStatement` transactions.

---

## 👥 2. Team Contributions & Division of Work

| Developer | Core Responsibilities | Key Classes & Deliverables |
| :--- | :--- | :--- |
| **VARIJ** | Agent Management, Multi-Agent Orchestration, AI Gateway, Concurrent Workflow Execution. | `TaskOrchestrator`, `AgentConfig`, `AgentRole`, `AgentService`, `AIService`, `OfflineHeuristicAIService`, `RemoteHttpAIService`. |
| **ASHUTOSH** | Database Architecture, MySQL JDBC, Schema DDL, DAO Layer, Transactions. | `DatabaseConnectionFactory`, `TransactionManager`, `ProjectDao`, `TaskDao`, `AgentDao`, `KnowledgeDao`, `DocumentDao`, `db.properties`. |
| **SANCHIT** | Project Management, Task Domain, Document Workflows, Terminal CLI. | `ProjectService`, `TaskService`, `DocumentWorkflowService`, `KnowledgeService`, `PharoahCLI`, `PharoahApplication`. |

---

## 🏛️ 3. Architecture & Layers

```text
Presentation Layer (Terminal CLI Only)
       │
Service Layer (ProjectService, TaskService, AgentService, KnowledgeService, DocumentWorkflowService)
       │
Orchestration & Concurrency Layer (TaskOrchestrator, ThreadPoolManager, BoundedTaskQueue)
       │
DAO Layer (ProjectDao, TaskDao, AgentDao, KnowledgeDao, DocumentDao)
       │
JDBC Persistence (DatabaseConnectionFactory with db.properties, TransactionManager)
       │
MySQL 8.0 Database (pharoah_db on localhost:3306)
```

---

## ⚡ 4. How to Compile and Run

### Prerequisites
* Java JDK 21 or later installed (`javac -version`, `java -version`).
* MySQL 8.0 running locally on port 3306 (configured in `db.properties`).

### 1. Interactive Terminal Mode
Launch the full interactive command-line workspace:
```powershell
cd "c:\Users\varij chauhan\OneDrive\Desktop\pharoah"
javac -cp "lib\*" -d target\classes (Get-ChildItem -Path src\main\java -Filter *.java -Recurse | Select-Object -ExpandProperty FullName)
java -cp "target\classes;lib\*" com.pharoah.app.PharoahApplication
```

### 2. Automated Demonstration Mode (For Viva / Lab Evaluation)
Runs an automated, end-to-end walkthrough demonstrating all 6 core modules:
```powershell
java -cp "target\classes;lib\*" com.pharoah.app.PharoahApplication --demo
```

### 3. Quick System Status Check
Inspects database connectivity, active project, and thread pool size:
```powershell
java -cp "target\classes;lib\*" com.pharoah.app.PharoahApplication status
```

---

## 🧪 5. Educational Test Suites

Run any syllabus concept demo independently:
```powershell
# 1. OOP & Collections Framework (PriorityQueue heap, HashMap O(1))
java -cp "target\classes;lib\*" -ea com.pharoah.Milestone1Demo

# 2. Multithreading (Thread lifecycle, wait/notify bounded queue, ExecutorService)
java -cp "target\classes;lib\*" -ea com.pharoah.ConcurrencyDemo

# 3. JDBC & Transactions (PreparedStatement, commit/rollback)
java -cp "target\classes;lib\*" -ea com.pharoah.JdbcPersistenceDemo

# 4. Stream API & Analytics (Collectors.groupingBy, summarizingDouble)
java -cp "target\classes;lib\*" -ea com.pharoah.FunctionalStreamsDemo

# 5. Generics & File I/O (Repository<T, ID>, PECS wildcards, StreamTokenizer)
java -cp "target\classes;lib\*" -ea com.pharoah.GenericsFilesKnowledgeDemo

# 6. Multi-Agent Swarm Orchestration (Concurrent task delegation across 7 roles)
java -cp "target\classes;lib\*" -ea com.pharoah.AgentSwarmOrchestrationDemo
```
