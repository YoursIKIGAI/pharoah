package com.pharoah.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Factory class managing JDBC database connections and schema bootstrap.
 * 
 * Demonstrates Core Java JDBC Concepts:
 * 1. JDBC Architecture: Drivers, URLs, DriverManager, Connection, Statement.
 * 2. DatabaseMetaData: Inspecting underlying database capabilities and metadata dynamically.
 * 3. Exception-safe resource management via try-with-resources.
 * 4. DDL schema creation with foreign keys and indexes.
 */
public class DatabaseConnectionFactory {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:pharoah.db";
    private final String dbUrl;

    static {
        try {
            // Explicitly load the SQLite JDBC driver class into memory
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: SQLite JDBC driver class not found on classpath: " + e.getMessage());
        }
    }

    public DatabaseConnectionFactory(String dbUrl) {
        this.dbUrl = (dbUrl != null && !dbUrl.trim().isEmpty()) ? dbUrl : DEFAULT_DB_URL;
        initializeSchema();
    }

    public DatabaseConnectionFactory() {
        this(DEFAULT_DB_URL);
    }

    /**
     * Establishes and returns a live database connection.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        // Enable foreign key support in SQLite
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Prints database driver and system capabilities using DatabaseMetaData.
     */
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

    /**
     * Initializes the relational database schema if tables do not already exist.
     */
    private void initializeSchema() {
        String createSubjectsSql = """
            CREATE TABLE IF NOT EXISTS subjects (
                subject_id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(100) NOT NULL UNIQUE,
                description TEXT,
                created_at TIMESTAMP NOT NULL
            );
        """;

        String createTasksSql = """
            CREATE TABLE IF NOT EXISTS tasks (
                task_id VARCHAR(36) PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                task_type VARCHAR(50) NOT NULL,
                priority VARCHAR(20) NOT NULL,
                status VARCHAR(30) NOT NULL,
                input_payload TEXT NOT NULL,
                final_output TEXT,
                subject_id VARCHAR(36),
                created_at TIMESTAMP NOT NULL,
                completed_at TIMESTAMP,
                FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE SET NULL
            );
        """;

        String createExecutionsSql = """
            CREATE TABLE IF NOT EXISTS agent_executions (
                execution_id VARCHAR(36) PRIMARY KEY,
                task_id VARCHAR(36) NOT NULL,
                agent_name VARCHAR(100) NOT NULL,
                status VARCHAR(30) NOT NULL,
                duration_ms INTEGER NOT NULL,
                execution_log TEXT,
                executed_at TIMESTAMP NOT NULL,
                FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createSubjectsSql);
            stmt.execute(createTasksSql);
            stmt.execute(createExecutionsSql);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }
}
