# 👑 PHAROAH: Multi-Agent Orchestration & Concurrency System

**Platform:** Java 21 LTS (FixedThreadPool, CompletableFuture, ReentrantLock, Concurrent Collections)  
**Persistence:** MySQL 8.0 (JDBC) with automatic SQLite fallback  
**Interface:** Pure Terminal Command-Line Application (Strictly No web server, No GUI)  
**Developers:** Varij Chauhan, Ashutosh, Sanchit (BTech CSE, University Pair-Programming Project)

---

## 📖 1. Project Overview

**PHAROAH** is a Java-native multi-agent orchestration platform designed for complex intellectual and technical writing workflows. Instead of acting as a simple chatbot or running sequential prompts, PHAROAH empowers users to configure, schedule, and orchestrate specialized AI agents using Java's multithreading and concurrency primitives.

### The 4 Execution Stages:
1. **Stage 1 (Concurrent Initial Execution):** Independent agents (0 dependencies) dispatch and execute simultaneously across worker threads.
2. **Stage 2 (Dependent Execution):** Dependent agents wait in state `[!] WAITING` until all prerequisite upstream outputs are published to the thread-safe `WorkflowContext`.
3. **Stage 3 (Parallel Execution):** Newly unblocked downstream agents execute in parallel according to user-defined priority (`HIGH` $\rightarrow$ `MEDIUM` $\rightarrow$ `LOW`).
4. **Stage 4 (Final Synthesis & Persistence):** A coordinator reviews all outputs for structural consistency, writes the compiled artifact to `outputs/`, and logs an audit row into MySQL table `pharoah_db.user_activity`.

---

## 👥 2. Team Contributions & Division of Work

| Developer | Core Responsibilities | Key Classes & Deliverables |
| :--- | :--- | :--- |
| **VARIJ** | Multi-Agent Orchestration Engine, DAG Scheduling, Java Concurrency, Terminal Dashboard. | `WorkflowOrchestrator`, `WorkflowPlan`, `AgentDeployment`, `WorkflowContext`, `DashboardView`, `SpecializedAgentTask`, `PharoahCLI`, `TerminalUI`. |
| **ASHUTOSH** | Category Workflows, Swarm Roles, Domain Synthesis, Python Verification Tests. | `WorkflowCategory`, `python/agents/*`, `python/workflows/*`, `python/main.py`. |
| **SANCHIT** | Database Persistence, Centralized Configuration, MySQL Schema, JDBC Connection Pooling, User Activity DAO. | `AppConfig`, `DatabaseConnectionFactory`, `UserActivityDAO`, `UserActivity`, `config.properties`, `config.properties.example`, `database/schema.sql`. |

---

## 🏛️ 3. Architecture & Layers

```text
========================================================================
                      PHAROAH SYSTEM ARCHITECTURE
========================================================================

 [ TERMINAL PRESENTATION & DASHBOARD ]
          │  PharoahCLI & DashboardView
          │  Dynamic ANSI thread dashboard, progress bars, real-time states
          ▼
 [ CENTRALIZED CONFIGURATION ]
          │  AppConfig (Singleton): config.properties + Environment Variables
          │  Zero hardcoded credentials in Java code
          ▼
 [ MULTI-AGENT ORCHESTRATION & CONCURRENCY ]
          │  WorkflowOrchestrator & WorkflowPlan
          │  Kahn's Topological DAG validation & priority scheduling
          │  Java FixedThreadPool (WorkerPool-X)
          │  ReentrantLock, Condition, AtomicInteger, ConcurrentHashMap
          │  Inter-agent data sharing via WorkflowContext
          ▼
 [ PERSISTENCE & DATABASE LAYER ]
          │  Standard JDBC PreparedStatement Transactions (UserActivityDAO)
          │  MySQL 8.0 Database (pharoah_db.user_activity)
          │  Automatic SQLite fallback (pharoah.db)
========================================================================
```

---

## ⚡ 4. How to Compile and Run

### Prerequisites
* **Java JDK 21+** (`javac -version`, `java -version`).
* **MySQL 8.0** running locally on port 3306 with database `pharoah_db`.
* (Optional) **Python 3.8+** for external swarm self-tests.

### 1. Interactive Terminal Mode
Launch the full interactive command-line workspace:
```powershell
cd "c:\Users\varij chauhan\OneDrive\Desktop\pharoah"
javac -cp "lib/*" -d target/classes (Get-ChildItem -Path src/main/java -Filter *.java -Recurse | Select-Object -ExpandProperty FullName)
java -cp "target/classes;lib/*" com.pharoah.app.PharoahApplication
```

### 2. Automated Demonstration Mode (For Viva / Lab Evaluation)
Runs an automated, end-to-end walkthrough demonstrating genuine Java multithreaded orchestration across 6 agents:
```powershell
java -cp "target/classes;lib/*" com.pharoah.app.PharoahApplication --demo
```

### 3. Quick System Health & Config Check
Inspects database connectivity, configuration parameters, and thread pool size:
```powershell
java -cp "target/classes;lib/*" com.pharoah.app.PharoahApplication status
```

### 4. MySQL Workbench Inspection
Open **MySQL Workbench** and execute:
```sql
USE pharoah_db;
SELECT * FROM user_activity ORDER BY created_at DESC;
```

---

## 📑 5. Supported Workflow Categories

1. **Research Paper**: Literature Review Agent, Data Collection Agent, Methodology Agent, Introduction Agent, Discussion Agent, Formatting & References Agent.
2. **Creative Writing**: World-Building & Lore Agent, Character Conception Agent, Plot Development Agent, Scene & Dialogue Agent, Style & Editorial Agent.
3. **Technical Documentation**: Requirements Analysis Agent, Architecture Design Agent, Code Specification Agent, API Documentation Agent, Audit & Formatting Agent.
4. **Business Report**: Executive Summary Agent, Market Research Agent, Solution Strategy Agent, Financial Modeling Agent, Governance & Risk Agent.
5. **Custom Workflow**: Fully user-defined agents, custom responsibilities, individual priorities, and explicit dependency mappings.

---

## 🔒 6. Centralized Configuration & Security

Configuration is managed via [`config.properties`](file:///c:/Users/varij%20chauhan/OneDrive/Desktop/pharoah/config.properties) (template provided in [`config.properties.example`](file:///c:/Users/varij%20chauhan/OneDrive/Desktop/pharoah/config.properties.example)).

Environment variables take precedence over configuration files for secure deployments:
* `DB_URL` / `MYSQL_URL` (default: `jdbc:mysql://localhost:3306/pharoah_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true`)
* `DB_USER` / `MYSQL_USER` (default: `root`)
* `DB_PASSWORD` / `MYSQL_PASSWORD` (checks environment variable first, then `config.properties`)
* `UI_ANSI` (default: `true`)

If MySQL is unavailable, PHAROAH automatically activates an in-process SQLite fallback (`pharoah.db`) to ensure zero-crash robustness. Sensitive passwords are never hardcoded in Java source files.
