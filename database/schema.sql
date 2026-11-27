-- =========================================================================
-- PHAROAH Multi-Agent Orchestration Database Schema (MySQL 8.0)
-- Host: 127.0.0.1 | Port: 3306 | Database: pharoah_db | Username: root
-- Run this script in MySQL Workbench or let PHAROAH auto-apply on startup.
-- =========================================================================

CREATE DATABASE IF NOT EXISTS pharoah_db;
USE pharoah_db;

-- 1. Subjects Table
CREATE TABLE IF NOT EXISTS subjects (
    subject_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Projects Table
CREATE TABLE IF NOT EXISTS projects (
    project_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. Agents Registry Table
CREATE TABLE IF NOT EXISTS agents (
    agent_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    instructions TEXT,
    provider_type VARCHAR(50) NOT NULL DEFAULT 'MOCK_HEURISTIC',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 4. Tasks Table
CREATE TABLE IF NOT EXISTS tasks (
    task_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    input_payload TEXT,
    assigned_agent_id VARCHAR(64),
    depends_on_task_id VARCHAR(64),
    final_output MEDIUMTEXT,
    review_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL
) ENGINE=InnoDB;

-- 5. Knowledge Resources Table
CREATE TABLE IF NOT EXISTS knowledge_resources (
    resource_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    uri_or_path VARCHAR(500) NOT NULL,
    tags VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 6. Documents Table
CREATE TABLE IF NOT EXISTS documents (
    document_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    target_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 7. User Activity Table (Execution Audit)
CREATE TABLE IF NOT EXISTS user_activity (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    workflow_type VARCHAR(100) NOT NULL,
    sections_worked TEXT NOT NULL,
    agents_used TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 8. Master Runs Table (Prompt-Driven Multi-Agent Workflows)
CREATE TABLE IF NOT EXISTS runs (
    run_id VARCHAR(64) PRIMARY KEY,
    user_prompt TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL
) ENGINE=InnoDB;

-- 9. Detailed Agent Runs Table (Parallel Worker Executions & Metrics)
CREATE TABLE IF NOT EXISTS agent_runs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    thread_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    duration_ms BIGINT NOT NULL,
    prompt_chars INT NOT NULL DEFAULT 0,
    output_chars INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (run_id) REFERENCES runs(run_id) ON DELETE CASCADE
) ENGINE=InnoDB;
