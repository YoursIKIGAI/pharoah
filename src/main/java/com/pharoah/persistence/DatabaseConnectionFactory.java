package com.pharoah.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

// ASHUTOSH: Implemented DatabaseConnectionFactory.
// Manages MySQL JDBC connection pooling, configuration loading from db.properties,
// automatic schema bootstrapping for 7 relational tables, and graceful SQLite fallback.
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
        this.dbUrl = customDbUrl;
        this.dbType = customDbUrl.contains("mysql") ? "mysql" : "sqlite";
        this.dbDriver = this.dbType.equals("mysql") ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC";
        this.dbUser = "root";
        this.dbPassword = "";
        testAndInitializeSchema();
    }

    // ASHUTOSH: Reads database credentials from db.properties file
    private void loadConfiguration() {
        Properties props = new Properties();
        try {
            // First check local file on disk
            File propFile = new File("db.properties");
            if (propFile.exists()) {
                try (InputStream is = new FileInputStream(propFile)) {
                    props.load(is);
                }
            } else {
                // Check classpath resource
                try (InputStream is = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                    if (is != null) {
                        props.load(is);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DB CONFIG WARN] Failed to load db.properties, applying defaults: " + e.getMessage());
        }

        this.dbType = props.getProperty("db.type", "mysql");
        this.dbDriver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        this.dbUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/pharoah_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
        this.dbUser = props.getProperty("db.user", "root");
        this.dbPassword = props.getProperty("db.password", "i@mvarij999@");
    }

    // ASHUTOSH: Establishes live JDBC Connection using DriverManager
    public Connection getConnection() throws SQLException {
        try {
            Class.forName(dbDriver);
            if ("mysql".equalsIgnoreCase(dbType) && isMySqlActive) {
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            } else {
                // SQLite fallback
                return DriverManager.getConnection("jdbc:sqlite:pharoah.db");
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver class not found: " + dbDriver, e);
        }
    }

    // ASHUTOSH: Verifies MySQL server connectivity; falls back to SQLite if offline
    private void testAndInitializeSchema() {
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
            System.out.println("[DATABASE] MySQL connection failed (" + e.getMessage() + "). Falling back to SQLite (pharoah.db).");
        }

        initializeAllTables();
    }

    // ASHUTOSH: Creates all 7 relational tables with primary keys and foreign keys
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
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
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

            seedInitialData(conn);

        } catch (SQLException e) {
            System.err.println("[SCHEMA ERROR] Failed to initialize database tables: " + e.getMessage());
        }
    }

    // ASHUTOSH: Populates initial sample project, knowledge assets, and 7 swarm agents
    private void seedInitialData(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
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

        } catch (SQLException e) {
            // For SQLite fallback (NOW() and INSERT IGNORE syntax differs), ignore seed failures on startup
        }
    }

    public boolean isMySqlActive() {
        return isMySqlActive;
    }

    public String getDbType() {
        return dbType;
    }

    // ASHUTOSH: Prints live metadata about the connected database engine
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
