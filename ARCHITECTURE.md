# 🏛️ PHAROAH: System Architecture & Technical Specifications

**Architecture Style:** Layered Hexagonal Architecture (Terminal-First)  
**Database Engine:** MySQL 8.0 (InnoDB) with SQLite fallback  
**Platform:** Java 21 LTS

---

## 1. Architectural Layers

PHAROAH follows a clean 4-tier layered architecture:

```mermaid
flowchart TD
    subgraph Presentation ["Presentation Layer"]
        CLI["PharoahCLI (Terminal Application)"]
    end

    subgraph Service ["Service Layer (Business Logic)"]
        PS["ProjectService"]
        TS["TaskService"]
        AS["AgentService"]
        KS["KnowledgeService"]
        DS["DocumentWorkflowService"]
    end

    subgraph Core ["Orchestration & Concurrency Layer"]
        ORCH["TaskOrchestrator"]
        POOL["ThreadPoolManager (ExecutorService)"]
        QUEUE["BoundedTaskQueue (wait/notify)"]
        AI["AIService (Offline Heuristic / Cloud LLM)"]
        ORCH --> POOL
        ORCH --> QUEUE
        ORCH --> AI
    end

    subgraph DAO ["DAO Layer (Data Access Objects)"]
        PDAO["ProjectDao"]
        TDAO["TaskDao"]
        ADAO["AgentDao"]
        KDAO["KnowledgeDao"]
        DDAO["DocumentDao"]
    end

    subgraph Persistence ["Persistence Layer (JDBC)"]
        CF["DatabaseConnectionFactory (db.properties)"]
        TM["TransactionManager (commit/rollback)"]
        DB[("MySQL Database: pharoah_db")]
        CF --> DB
        TM --> DB
    end

    Presentation --> Service
    Service --> Core
    Service --> DAO
    Core --> DAO
    DAO --> Persistence
```

---

## 2. Relational Database Schema (MySQL 8.0)

```mermaid
erDiagram
    PROJECTS ||--o{ TASKS : contains
    PROJECTS ||--o{ DOCUMENTS : owns
    PROJECTS ||--o{ KNOWLEDGE_RESOURCES : stores
    AGENTS ||--o{ TASKS : assigned_to
    DOCUMENTS ||--o{ DOCUMENT_SECTIONS : composed_of
    AGENTS ||--o{ DOCUMENT_SECTIONS : drafts

    PROJECTS {
        VARCHAR project_id PK
        VARCHAR name UK
        TEXT description
        VARCHAR status
        TIMESTAMP created_at
    }

    AGENTS {
        VARCHAR agent_id PK
        VARCHAR name UK
        VARCHAR role
        TEXT instructions
        VARCHAR provider_type
        BOOLEAN is_active
        TIMESTAMP created_at
    }

    TASKS {
        VARCHAR task_id PK
        VARCHAR project_id FK
        VARCHAR title
        VARCHAR task_type
        VARCHAR priority
        VARCHAR status
        TEXT input_payload
        VARCHAR assigned_agent_id FK
        VARCHAR depends_on_task_id FK
        MEDIUMTEXT final_output
        TEXT review_notes
        TIMESTAMP created_at
        TIMESTAMP completed_at
    }

    DOCUMENTS {
        VARCHAR document_id PK
        VARCHAR project_id FK
        VARCHAR title
        VARCHAR document_type
        VARCHAR status
        TIMESTAMP created_at
    }

    DOCUMENT_SECTIONS {
        VARCHAR section_id PK
        VARCHAR document_id FK
        VARCHAR section_name
        INT section_order
        VARCHAR assigned_agent_id FK
        MEDIUMTEXT content
        VARCHAR status
        INT version
        TIMESTAMP updated_at
    }

    KNOWLEDGE_RESOURCES {
        VARCHAR resource_id PK
        VARCHAR project_id FK
        VARCHAR title
        VARCHAR resource_type
        MEDIUMTEXT content_or_url
        VARCHAR topic_tag
        TIMESTAMP created_at
    }

    WORKFLOW_LOGS {
        VARCHAR log_id PK
        VARCHAR task_id FK
        VARCHAR agent_id FK
        VARCHAR action
        TEXT details
        TIMESTAMP created_at
    }
```

---

## 3. Multi-Agent Orchestration Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as Student / Evaluator
    participant CLI as PharoahCLI (Terminal)
    participant Orch as TaskOrchestrator
    participant Pool as ThreadPoolManager
    participant Agent as Specialized Agent
    participant AI as AIService (Mock / Gemini)
    participant DAO as TaskDao (MySQL)

    User->>CLI: Submit Objective ("Decompose Research Goal")
    CLI->>Orch: decomposeObjective(projectId, title, topic)
    Orch->>DAO: Insert subtasks (Literature Review, Analysis, Report, Review)
    Orch-->>CLI: Display 4 subtasks with dependency graph

    User->>CLI: Execute Task ("Literature Review")
    CLI->>Orch: submitTaskAsync(task)
    Orch->>Orch: Check Prerequisite Dependencies (None for task 1)
    Orch->>Pool: Submit Callable to Worker Thread
    Pool->>Agent: executeIntelligence(task, agentInstructions)
    Agent->>AI: synthesizeResearch(prompt, context)
    AI-->>Agent: Synthesized findings & research gaps
    Agent-->>Pool: TaskResult(output, latencyMs, agentName)
    Pool->>DAO: Update Task (status = UNDER_REVIEW)
    Pool-->>CLI: Display output on Terminal Screen

    User->>CLI: Inspect Output & Enter Feedback
    CLI->>DAO: recordReviewApproval(taskId, approved=true, reviewNotes)
    DAO-->>User: Status updated to COMPLETED (Approved by Human)
```

---

## 4. Document Workflow Engine

1. **Template Generation:** When a user creates an *Academic Research Paper*, *Technical Blog*, *Technical Documentation*, or *Business Proposal*, `DocumentWorkflowService` automatically creates the corresponding standard sections.
2. **Independent Section Drafting:** Individual sections are assigned to specialized agents (e.g. `Abstract` to `WRITING_AGENT`, `Methodology` to `RESEARCH_AGENT`, `Architecture` to `DOCUMENTATION_AGENT`).
3. **Section-Level Review & Versioning:** Each section maintains an independent `version` counter and status (`PENDING` $\to$ `DRAFTED` $\to$ `UNDER_REVIEW` $\to$ `APPROVED`).
4. **Final Compilation:** Calling `compileDocument()` reads all approved sections in their defined `section_order` and concatenates them into a unified Markdown document.
