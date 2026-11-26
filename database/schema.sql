-- =========================================================================
-- ASHUTOSH: PHAROAH - Simplified Database Schema
-- Run this script in MySQL Workbench or via the MySQL command line.
-- Maintains a clean, persistent audit of user intellectual workflows.
-- =========================================================================

CREATE DATABASE IF NOT EXISTS pharoah_db;
USE pharoah_db;

-- ASHUTOSH: User Activity Table
-- Records user, topic, selected workflow, sections worked on, and agents used.
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
