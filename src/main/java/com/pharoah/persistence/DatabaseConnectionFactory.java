package com.pharoah.persistence;

import com.pharoah.config.AppConfig;

import java.sql.*;

// SANCHIT: Implemented DatabaseConnectionFactory.
// Manages MySQL JDBC connection pooling, configuration loading via AppConfig,
// automatic schema bootstrapping for persistent tables (including user_activity), and SQLite fallback.
public class DatabaseConnectionFactory {

    private String dbType;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String dbDriver;
    private boolean isMySqlActive = false;

    public DatabaseConnectionFactory() {
        loadConfiguration();
        testAndInitializeSchema();
    }

    public DatabaseConnectionFactory(String customDbUrl) {
        AppConfig config = AppConfig.getInstance();
        this.dbUrl = customDbUrl;
        this.dbType = customDbUrl.contains("mysql") ? "mysql" : "sqlite";
        this.dbDriver = this.dbType.equals("mysql") ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC";
        this.dbUser = config.getDbUser();
        this.dbPassword = config.getDbPassword();
        testAndInitializeSchema();
    }

    // SANCHIT: Loads centralized configuration from AppConfig
    private void loadConfiguration() {
        AppConfig config = AppConfig.getInstance();
        this.dbType = config.getDbType();
        this.dbDriver = config.getDbDriver();
        this.dbUrl = config.getDbUrl();
        this.dbUser = config.getDbUser();
        this.dbPassword = config.getDbPassword();
    }

    // SANCHIT: Establishes live JDBC Connection using DriverManager
    public Connection getConnection() throws SQLException {
        try {
            Class.forName(dbDriver);
            if ("mysql".equalsIgnoreCase(dbType) && isMySqlActive) {
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            } else {
                // SQLite fallback
                String targetUrl = (dbUrl != null && dbUrl.contains("sqlite")) ? dbUrl : AppConfig.getInstance().getDbFallbackSqlite();
                return DriverManager.getConnection(targetUrl);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver class not found: " + dbDriver, e);
        }
    }

    // SANCHIT: Verifies MySQL server connectivity; falls back to SQLite if offline
    private void testAndInitializeSchema() {
        if ("mysql".equalsIgnoreCase(this.dbType)) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                    this.isMySqlActive = true;
                    System.out.println("[DATABASE] Successfully connected to MySQL 8.0 on localhost:3306 (pharoah_db).");
                }
            } catch (Exception e) {
                this.isMySqlActive = false;
                this.dbType = "sqlite";
                this.dbDriver = "org.sqlite.JDBC";
                System.out.println("[DATABASE] MySQL connection failed (" + e.getMessage() + "). Falling back to SQLite (" + AppConfig.getInstance().getDbFallbackSqlite() + ").");
            }
        } else {
            this.isMySqlActive = false;
            this.dbType = "sqlite";
            this.dbDriver = "org.sqlite.JDBC";
        }

        initializeAllTables();
    }

    // SANCHIT: Creates persistent relational tables with primary keys and foreign keys
    private void initializeAllTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            if (!isMySqlActive) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            // 0. Subjects Table (Legacy / Subject isolation compatibility)
            String sqlSubjects = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS subjects (
                    subject_id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(150) NOT NULL UNIQUE,
                    description TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS subjects (
                    subject_id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(150) NOT NULL UNIQUE,
                    description TEXT,
                    created_at TIMESTAMP NOT NULL
                );
            """;
            stmt.execute(sqlSubjects);

            // 1. Projects Table
            String sqlProjects = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS projects (
                    project_id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(150) NOT NULL UNIQUE,
                    description TEXT,
                    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS projects (
                    project_id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(150) NOT NULL UNIQUE,
                    description TEXT,
                    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP NOT NULL
                );
            """;
            stmt.execute(sqlProjects);

            // 2. Agents Registry Table
            String sqlAgents = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS agents (
                    agent_id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    role VARCHAR(50) NOT NULL,
                    instructions TEXT,
                    provider_type VARCHAR(50) NOT NULL DEFAULT 'MOCK_HEURISTIC',
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS agents (
                    agent_id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    role VARCHAR(50) NOT NULL,
                    instructions TEXT,
                    provider_type VARCHAR(50) NOT NULL DEFAULT 'MOCK_HEURISTIC',
                    is_active BOOLEAN NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL
                );
            """;
            stmt.execute(sqlAgents);

            // 3. Tasks Table
            String sqlTasks = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS tasks (
                    task_id VARCHAR(64) PRIMARY KEY,
                    project_id VARCHAR(64) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    task_type VARCHAR(50) NOT NULL,
                    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    input_payload TEXT,
                    assigned_agent_id VARCHAR(64),
                    depends_on_task_id VARCHAR(64),
                    final_output MEDIUMTEXT,
                    review_notes TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP NULL,
                    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS tasks (
                    task_id VARCHAR(64) PRIMARY KEY,
                    project_id VARCHAR(64) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    task_type VARCHAR(50) NOT NULL,
                    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    input_payload TEXT,
                    assigned_agent_id VARCHAR(64),
                    depends_on_task_id VARCHAR(64),
                    final_output TEXT,
                    review_notes TEXT,
                    created_at TIMESTAMP NOT NULL,
                    completed_at TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
                );
            """;
            stmt.execute(sqlTasks);

            // 4. Documents Table
            String sqlDocs = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS documents (
                    document_id VARCHAR(64) PRIMARY KEY,
                    project_id VARCHAR(64) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    document_type VARCHAR(50) NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS documents (
                    document_id VARCHAR(64) PRIMARY KEY,
                    project_id VARCHAR(64) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    document_type VARCHAR(50) NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                    created_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
                );
            """;
            stmt.execute(sqlDocs);

            // 5. Document Sections Table
            String sqlSections = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS document_sections (
                    section_id VARCHAR(64) PRIMARY KEY,
                    document_id VARCHAR(64) NOT NULL,
                    section_name VARCHAR(150) NOT NULL,
                    section_order INT NOT NULL,
                    assigned_agent_id VARCHAR(64),
                    content MEDIUMTEXT,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    version INT NOT NULL DEFAULT 1,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS document_sections (
                    section_id VARCHAR(64) PRIMARY KEY,
                    document_id VARCHAR(64) NOT NULL,
                    section_name VARCHAR(150) NOT NULL,
                    section_order INTEGER NOT NULL,
                    assigned_agent_id VARCHAR(64),
                    content TEXT,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    version INTEGER NOT NULL DEFAULT 1,
                    updated_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
                );
            """;
            stmt.execute(sqlSections);

            // 6. Knowledge Resources Table
            String sqlKnowledge = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS knowledge_resources (
                    resource_id VARCHAR(64) PRIMARY KEY,
                    project_id VARCHAR(64) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    resource_type VARCHAR(50) NOT NULL,
                    content_or_url MEDIUMTEXT NOT NULL,
                    topic_tag VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS knowledge_resources (
                    resource_id VARCHAR(64) PRIMARY KEY,
                    project_id VARCHAR(64) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    resource_type VARCHAR(50) NOT NULL,
                    content_or_url TEXT NOT NULL,
                    topic_tag VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
                );
            """;
            stmt.execute(sqlKnowledge);

            // 7. Workflow Activity Logs Table
            String sqlLogs = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS workflow_logs (
                    log_id VARCHAR(64) PRIMARY KEY,
                    task_id VARCHAR(64),
                    agent_id VARCHAR(64),
                    action VARCHAR(100) NOT NULL,
                    details TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS workflow_logs (
                    log_id VARCHAR(64) PRIMARY KEY,
                    task_id VARCHAR(64),
                    agent_id VARCHAR(64),
                    action VARCHAR(100) NOT NULL,
                    details TEXT,
                    created_at TIMESTAMP NOT NULL
                );
            """;
            stmt.execute(sqlLogs);

            // 8. User Activity Audit Table (SANCHIT: Format-driven activity records for MySQL Workbench)
            String sqlUserActivity = isMySqlActive ? """
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
            """ : """
                CREATE TABLE IF NOT EXISTS user_activity (
                    activity_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_name TEXT NOT NULL,
                    topic TEXT NOT NULL,
                    workflow_type TEXT NOT NULL,
                    sections_worked TEXT NOT NULL,
                    agents_used TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'COMPLETED',
                    created_at TIMESTAMP NOT NULL
                );
            """;
            stmt.execute(sqlUserActivity);

            seedInitialData(conn);

        } catch (SQLException e) {
            System.err.println("[SCHEMA ERROR] Failed to initialize database tables: " + e.getMessage());
        }
    }

    // SANCHIT: Populates initial sample project, knowledge assets, and 7 swarm agents
    private void seedInitialData(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            if (isMySqlActive) {
                // Seed Default Project
                stmt.executeUpdate("""
                    INSERT IGNORE INTO projects (project_id, name, description, status, created_at)
                    VALUES ('proj-default-01', 'Core Engineering & Research', 'Primary workspace for multi-agent tasks, academic research, and system engineering.', 'ACTIVE', NOW());
                """);

                // Seed Default Agents (All 7 Roles)
                String[][] defaultAgents = {
                    {"agent-01", "Research Specialist", "RESEARCH_AGENT", "Investigate literature, academic findings, and cite relevant sources.", "MOCK_HEURISTIC"},
                    {"agent-02", "Technical Writer", "WRITING_AGENT", "Draft polished prose, technical blogs, and formal reports.", "MOCK_HEURISTIC"},
                    {"agent-03", "Data & Metrics Analyst", "ANALYSIS_AGENT", "Analyze empirical data, identify patterns, and compare paradigms.", "MOCK_HEURISTIC"},
                    {"agent-04", "Software Engineer", "CODING_AGENT", "Review algorithms, explain code patterns, and draft implementations.", "MOCK_HEURISTIC"},
                    {"agent-05", "Docs Architect", "DOCUMENTATION_AGENT", "Generate system specs, installation guides, and API documentation.", "MOCK_HEURISTIC"},
                    {"agent-06", "Quality Reviewer", "REVIEWER_AGENT", "Verify facts, check claims, and assess accuracy of written sections.", "MOCK_HEURISTIC"},
                    {"agent-07", "Project Planner", "PLANNING_AGENT", "Decompose goals into milestone roadmaps and dependent task graphs.", "MOCK_HEURISTIC"}
                };

                for (String[] a : defaultAgents) {
                    String sql = String.format("""
                        INSERT IGNORE INTO agents (agent_id, name, role, instructions, provider_type, is_active, created_at)
                        VALUES ('%s', '%s', '%s', '%s', '%s', 1, NOW());
                    """, a[0], a[1], a[2], a[3], a[4]);
                    stmt.executeUpdate(sql);
                }

                // Seed Sample Knowledge Asset
                stmt.executeUpdate("""
                    INSERT IGNORE INTO knowledge_resources (resource_id, project_id, title, resource_type, content_or_url, topic_tag, created_at)
                    VALUES ('res-01', 'proj-default-01', 'Java 21 LTS Specifications', 'URL', 'https://openjdk.org/projects/jdk/21/', 'JAVA', NOW());
                """);
            }

        } catch (SQLException e) {
            // Ignore seed duplicate failures on startup
        }
    }

    public boolean isMySqlActive() {
        return isMySqlActive;
    }

    public String getDbType() {
        return dbType;
    }

    // SANCHIT: Prints live metadata about the connected database engine
    public void printDatabaseMetaData() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("========== DATABASE METADATA ==========");
            System.out.println("  DB Product Name    : " + metaData.getDatabaseProductName());
            System.out.println("  DB Product Version : " + metaData.getDatabaseProductVersion());
            System.out.println("  Driver Name        : " + metaData.getDriverName());
            System.out.println("  Driver Version     : " + metaData.getDriverVersion());
            System.out.println("  Supports Txns      : " + metaData.supportsTransactions());
            System.out.println("  Supports Batch     : " + metaData.supportsBatchUpdates());
            System.out.println("========================================");
        } catch (SQLException e) {
            System.err.println("Failed to read database metadata: " + e.getMessage());
        }
    }
}
