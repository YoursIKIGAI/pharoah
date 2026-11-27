-- =========================================================================
-- ASHUTOSH & VARIJ: PHAROAH - Simplified Database Schema
-- Run this script in MySQL Workbench or via the MySQL command line.
-- Maintains a clean, persistent audit of user intellectual workflows,
-- prompt-driven runs, and individual agent executions.
-- =========================================================================

CREATE DATABASE IF NOT EXISTS pharoah_db;
USE pharoah_db;

-- 1. User Activity Table (Legacy / Format-Driven Workflows)
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

-- 2. Master Runs Table (Prompt-Driven Multi-Agent Workflows)
CREATE TABLE IF NOT EXISTS runs (
    run_id VARCHAR(64) PRIMARY KEY,
    user_prompt TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL
) ENGINE=InnoDB;

-- 3. Detailed Agent Runs Table (Parallel Worker Executions & Metrics)
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
