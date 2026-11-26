# 👑 PHAROAH: Format-Driven Multi-Agent Task Intelligence & Knowledge Management System

**Platform:** Java 21 LTS + Python 3.8+ Multi-Agent Swarm  
**Persistence:** MySQL 8.0 (JDBC) with automatic SQLite fallback  
**Interface:** Pure Terminal Command-Line Application (Strictly No web server, No GUI)  
**Developers:** Varij Chauhan, Ashutosh, Sanchit (BTech CSE, University Project)

---

## 📖 1. Project Overview

**PHAROAH** is an educational and enterprise-grade terminal-based multi-agent task execution and knowledge management platform. It transforms intellectual tasks (research, writing, technical analysis, data modeling) into format-driven, autonomous multi-agent pipelines with live execution streaming and persistent database logging.

### The 5-Step Format-Driven Workflow:
1. **Format Selection:** Choose from 8 standardized intellectual formats (*Research Paper, Research Analysis, Report Generation, Blog Writing, Technical Documentation, Business Proposal, Data Analysis, Custom Workflow*).
2. **Topic Scoping:** Define your target subject matter (e.g., *Impact of Artificial Intelligence on Cybersecurity*).
3. **Document Decomposition:** Display the format's section structure and choose to execute a single section, multiple sections, or the complete document.
4. **Priority Assignment:** Assign execution priorities (`HIGH`, `MEDIUM`, `LOW`) to determine sequence and resolve dependencies (e.g., Research Gap depends on Literature Review).
5. **Agent Allocation & Real-Time Swarm:** Match sections to specialized agents (*Research Specialist, Analysis Specialist, Technical Writer, Data & Metrics Analyst, Quality Reviewer*) and watch them execute live in the terminal!

---

## 👥 2. Team Contributions & Division of Work

| Developer | Core Responsibilities | Key Classes & Deliverables |
| :--- | :--- | :--- |
| **VARIJ** | Project Architecture, Java Orchestrator, CLI Terminal Interface, ProcessBuilder Bridge. | `PharoahCLI`, `TerminalUI`, `PythonAgentBridge`, `PythonExecutionResult`, `TaskOrchestrator`, `AgentService`, `ThreadPoolManager`. |
| **ASHUTOSH** | Python Multi-Agent Swarm, BaseAgent, Specialized Swarm Roles, Format Workflows, Execution Engine. | `python/agents/*`, `python/workflows/*`, `python/orchestrator/*`, `python/main.py`. |
| **SANCHIT** | Database Persistence, Centralized Configuration, MySQL Schema, JDBC Connection Pooling, User Activity DAO. | `AppConfig`, `DatabaseConnectionFactory`, `UserActivityDAO`, `UserActivity`, `config.properties`, `database/schema.sql`. |

---

## 🏛️ 3. Architecture & Layers

```text
========================================================================
                      PHAROAH SYSTEM ARCHITECTURE
========================================================================

 [ TERMINAL PRESENTATION LAYER ]
          │  Pure interactive command-line interface (PharoahCLI & TerminalUI)
          │  Breadcrumbs, status badges, and 5-step format-driven workflow
          ▼
 [ CENTRALIZED CONFIGURATION ]
          │  Single source of truth: config.properties + Environment Variables
          │  Zero hardcoded credentials in Java (AppConfig)
          ▼
 [ JAVA CORE & PROCESS BRIDGE ]
          │  Java 21 ProcessBuilder Subprocess Bridge (PythonAgentBridge)
          │  Streams live terminal outputs, logs, and token progress in real time
          ▼
 [ PYTHON MULTI-AGENT SWARM ]
          │  Main Orchestrator (MainAgent, WorkflowExecutor)
          │  Specialized Swarm:
          │    ├── Research Specialist (literature, empirical citations)
          │    ├── Analysis Specialist (research gaps, trade-offs)
          │    ├── Technical Writer (structured prose, introductions, conclusions)
          │    ├── Data & Metrics Analyst (statistical tables, latency metrics)
          │    └── Quality Reviewer (audit gatekeeper, quality scores)
          ▼
 [ PERSISTENCE & DATABASE LAYER ]
          │  Standard JDBC PreparedStatement Transactions (UserActivityDAO)
          │  MySQL 8.0 Database (pharoah_db.user_activity)
          │  Easily inspected in MySQL Workbench:
          │    SELECT * FROM pharoah_db.user_activity;
========================================================================
```

---

## ⚡ 4. How to Compile and Run

### Prerequisites
* **Java JDK 21+** (`javac -version`, `java -version`).
* **Python 3.8+** (`python --version`).
* **MySQL 8.0** running locally on port 3306 with database `pharoah_db`.

### 1. Interactive Terminal Mode
Launch the full interactive command-line workspace:
```powershell
cd "c:\Users\varij chauhan\OneDrive\Desktop\pharoah"
javac -cp "lib/*" -d target/classes (Get-ChildItem -Path src/main/java -Filter *.java -Recurse | Select-Object -ExpandProperty FullName)
java -cp "target/classes;lib/*" com.pharoah.app.PharoahApplication
```

### 2. Automated Demonstration Mode (For Viva / Lab Evaluation)
Runs an automated, end-to-end walkthrough demonstrating the format-driven multi-agent swarm and MySQL logging:
```powershell
java -cp "target/classes;lib/*" com.pharoah.app.PharoahApplication --demo
```

### 3. Quick System Health & Config Check
Inspects database connectivity, configuration parameters, and thread pool size:
```powershell
java -cp "target/classes;lib/*" com.pharoah.app.PharoahApplication status
```

### 4. Standalone Python Swarm Test
Run the multi-agent swarm directly in Python:
```powershell
python python/main.py --test
```

### 5. MySQL Workbench Inspection
Open **MySQL Workbench** and execute:
```sql
USE pharoah_db;
SELECT * FROM user_activity ORDER BY created_at DESC;
```

---

## 📑 5. Supported Document Formats & Workflows

1. **Research Paper**: Abstract, Introduction, Literature Review, Research Gap, Methodology, Data Analysis, Results and Discussion, Conclusion, References.
2. **Research Analysis**: Research Question, Literature Context, Data Collection, Evidence Analysis, Findings & Implications, Conclusion.
3. **Report Generation**: Executive Summary, Problem Statement, Current Observations, Technical Analysis, Recommendations, Next Steps.
4. **Blog Writing**: Title & Hook, Introduction, Core Concepts, Practical Examples & Code, Key Takeaways.
5. **Technical Documentation**: System Overview, Architecture & Modules, Installation & Setup, Configuration, API Reference, Troubleshooting & FAQ.
6. **Business Proposal**: Executive Summary, Market Problem, Proposed Solution, Implementation Roadmap, Budget & ROI Forecast, Conclusion & Call to Action.
7. **Data Analysis**: Analysis Objective, Data Ingestion & Scope, Statistical Metrics, Pattern Detection, Actionable Insights.
8. **Custom Workflow**: Dynamic user-defined sections mapped to intelligent swarm agents.

---

## 🔒 6. Centralized Configuration & Security

All connection parameters, credentials, python commands, and terminal styling are managed through a single centralized configuration file: [`config.properties`](file:///c:/Users/varij%20chauhan/OneDrive/Desktop/pharoah/config.properties).

Environment variables take precedence over configuration files for secure deployments:
* `DB_URL` / `MYSQL_URL` (default: `jdbc:mysql://localhost:3306/pharoah_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true`)
* `DB_USER` / `MYSQL_USER` (default: `root`)
* `DB_PASSWORD` / `MYSQL_PASSWORD` (checks environment variable first, then `config.properties`)
* `PYTHON_CMD` (default: `python`)
* `UI_ANSI` (default: `true`)

If MySQL is unavailable, PHAROAH automatically activates an in-process SQLite fallback (`pharoah.db`) to ensure zero-crash robustness. Sensitive passwords are never hardcoded in Java source files.
