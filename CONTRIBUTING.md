# 👥 PHAROAH: Team Contribution & Development Guidelines

This document tracks developer responsibilities, coding standards, and contribution attribution for our university software engineering evaluation.

---

## 1. Team Members & Module Ownership

### 👤 VARIJ (Agent Systems & Central Orchestrator)
* **Agent Management:** Configurable agent definitions, role registration, provider type assignment (`AgentConfig`, `AgentRole`, `AgentService`).
* **Multi-Agent Orchestration:** High-level objective decomposition into subtasks, dependency graph checks, and concurrent task dispatching (`TaskOrchestrator`).
* **AI Integration Layer:** Modular AI gateway (`AIService`), local offline heuristic engine (`OfflineHeuristicAIService`), and cloud LLM connector (`RemoteHttpAIService`).
* **Concurrent Execution:** Managed worker threads and agent execution timing (`AgentExecutionTask`, `ThreadPoolManager`).

### 👤 ASHUTOSH (Database Architecture & Persistence)
* **MySQL JDBC Connectivity:** Connection initialization, driver loading, and property configuration (`DatabaseConnectionFactory`, `db.properties`).
* **Relational Schema DDL:** Automated creation of 7 relational tables with primary keys, foreign key cascades, and InnoDB engine.
* **DAO Layer:** Data access objects with parameterized `PreparedStatement` queries (`ProjectDao`, `TaskDao`, `AgentDao`, `KnowledgeDao`, `DocumentDao`).
* **Transaction Management:** Atomic multi-table updates with `setAutoCommit(false)`, `commit()`, and `rollback()` (`TransactionManager`).

### 👤 SANCHIT (Project, Task, Document Workflows & Terminal CLI)
* **Project Management:** Project creation, workspace isolation, switching active contexts (`Project`, `ProjectService`).
* **Task Management:** Task creation, priority heap scheduling, human-in-the-loop review approval gates (`Task`, `TaskStatus`, `TaskService`).
* **Knowledge Repository:** Persistent storage and keyword/topic searching for URLs, notes, and code snippets (`KnowledgeResource`, `ResourceType`, `KnowledgeService`).
* **Document Workflow Management:** Multi-section document generation for 4 standard templates, section-level agent delegation, and final document compilation (`Document`, `DocumentSection`, `DocumentType`, `DocumentWorkflowService`).
* **Terminal Interface:** Interactive ANSI terminal shell and sub-menus (`PharoahCLI`, `PharoahApplication`).

---

## 2. In-Code Commenting Format

Every major class and method MUST contain comments identifying the developer responsible:

```java
// VARIJ: Implemented agent role validation.
// This method verifies that the requested agent is active and matches the task type.

// ASHUTOSH: Implemented MySQL connection handling.
// This method loads credentials from db.properties and establishes a JDBC Connection.

// SANCHIT: Implemented section-level document compilation.
// This method concatenates all approved document sections in order into a final markdown file.
```

---

## 3. Git Contribution Conventions

* Keep commit messages descriptive:  
  `[VARIJ] Implemented task dependency checking in TaskOrchestrator`  
  `[ASHUTOSH] Added MySQL foreign key constraints and ProjectDao CRUD`  
  `[SANCHIT] Built interactive document review sub-menu in PharoahCLI`
* Always compile and run tests before pushing:
  ```powershell
  javac -cp "lib\*" -d target\classes (Get-ChildItem -Path src -Filter *.java -Recurse | Select-Object -ExpandProperty FullName)
  java -cp "target\classes;lib\*" com.pharoah.app.PharoahApplication --demo
  ```
