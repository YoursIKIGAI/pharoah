package com.pharoah.persistence;

import com.pharoah.config.AppConfig;

import java.net.URI;
import java.sql.*;

// SANCHIT: Implemented DatabaseConnectionFactory.
// Manages MySQL JDBC connection pooling, configuration loading via AppConfig,
// automatic schema bootstrapping for persistent tables (including runs, agent_runs, and user_activity),
// and strict or graceful SQLite fallback.
public class DatabaseConnectionFactory {

    public static boolean requireMySql = false;

    private String dbType;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String dbDriver;
    private boolean isMySqlActive = false;
    private String connectionStatusBanner = "";

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

    private void loadConfiguration() {
        AppConfig config = AppConfig.getInstance();
        this.dbType = config.getDbType();
        this.dbDriver = config.getDbDriver();
        this.dbUrl = config.getDbUrl();
        this.dbUser = config.getDbUser();
        this.dbPassword = config.getDbPassword();
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName(dbDriver);
            if ("mysql".equalsIgnoreCase(dbType) && isMySqlActive) {
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            } else {
                String targetUrl = (dbUrl != null && dbUrl.contains("sqlite")) ? dbUrl : AppConfig.getInstance().getDbFallbackSqlite();
                return DriverManager.getConnection(targetUrl);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver class not found: " + dbDriver, e);
        }
    }

    private String extractHostAndPort(String jdbcUrl) {
        if (jdbcUrl == null) return "localhost:3306";
        try {
            String clean = jdbcUrl.replace("jdbc:mysql://", "http://");
            int slash = clean.indexOf('/', 7);
            if (slash != -1) {
                clean = clean.substring(0, slash);
            }
            URI uri = URI.create(clean);
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() != -1 ? uri.getPort() : 3306;
            return host + ":" + port;
        } catch (Exception ignored) {
            return "127.0.0.1:3306";
        }
    }

    private void testAndInitializeSchema() {
        String hostPort = extractHostAndPort(this.dbUrl);

        if ("mysql".equalsIgnoreCase(this.dbType)) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                    this.isMySqlActive = true;
                    this.connectionStatusBanner = "DATABASE: MySQL pharoah_db @ " + hostPort + " (connected)";
                    System.out.println(connectionStatusBanner);
                }
            } catch (Exception e) {
                this.isMySqlActive = false;
                this.connectionStatusBanner = "DATABASE: SQLite FALLBACK pharoah.db (MySQL NOT connected: " + e.getMessage() + ")";
                System.out.println(connectionStatusBanner);

                if (requireMySql) {
                    System.err.println("\n[FATAL] --require-mysql was specified but MySQL connection failed.");
                    System.err.println("Error: " + e.getMessage());
                    System.err.println("Please ensure MySQL 8.0 is running on " + hostPort + " and DB_PASSWORD is set.\n");
                    System.exit(1);
                }

                this.dbType = "sqlite";
                this.dbDriver = "org.sqlite.JDBC";
            }
        } else {
            this.isMySqlActive = false;
            this.dbType = "sqlite";
            this.dbDriver = "org.sqlite.JDBC";
            this.connectionStatusBanner = "DATABASE: SQLite pharoah.db (default mode)";
            System.out.println(connectionStatusBanner);
        }

        initializeAllTables();
    }

    private void initializeAllTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            if (!isMySqlActive) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            // 0. Subjects Table
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

            // 3. User Activity Audit Table
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

            // 4. Master Runs Table (Step 6)
            String sqlRuns = isMySqlActive ? """
                CREATE TABLE IF NOT EXISTS runs (
                    run_id VARCHAR(64) PRIMARY KEY,
                    user_prompt TEXT NOT NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
                    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    finished_at TIMESTAMP NULL
                ) ENGINE=InnoDB;
            """ : """
                CREATE TABLE IF NOT EXISTS runs (
                    run_id TEXT PRIMARY KEY,
                    user_prompt TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'COMPLETED',
                    started_at TIMESTAMP NOT NULL,
                    finished_at TIMESTAMP
                );
            """;
            stmt.execute(sqlRuns);

            // 5. Agent Runs Table (Step 6)
            String sqlAgentRuns = isMySqlActive ? """
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
            """ : """
                CREATE TABLE IF NOT EXISTS agent_runs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    run_id TEXT NOT NULL,
                    agent_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    model TEXT NOT NULL,
                    thread_name TEXT NOT NULL,
                    status TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    prompt_chars INTEGER NOT NULL DEFAULT 0,
                    output_chars INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (run_id) REFERENCES runs(run_id) ON DELETE CASCADE
                );
            """;
            stmt.execute(sqlAgentRuns);

            seedInitialData(conn);

        } catch (SQLException e) {
            System.err.println("[SCHEMA ERROR] Failed to initialize database tables: " + e.getMessage());
        }
    }

    private void seedInitialData(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            if (isMySqlActive) {
                stmt.executeUpdate("""
                    INSERT IGNORE INTO projects (project_id, name, description, status, created_at)
                    VALUES ('proj-default-01', 'Core Engineering & Research', 'Primary workspace for multi-agent tasks, academic research, and system engineering.', 'ACTIVE', NOW());
                """);
            }
        } catch (SQLException ignored) {}
    }

    public boolean isMySqlActive() {
        return isMySqlActive;
    }

    public String getDbType() {
        return dbType;
    }

    public String getConnectionStatusBanner() {
        return connectionStatusBanner;
    }

    public void printDatabaseStatus() {
        AppConfig cfg = AppConfig.getInstance();
        System.out.println("\n" + "=".repeat(76));
        System.out.println("PHAROAH: Database Status & Verification");
        System.out.println("=".repeat(76));
        System.out.println("Engine In Use    : " + (isMySqlActive ? "MySQL 8.0 (pharoah_db)" : "SQLite Fallback (pharoah.db)"));
        System.out.println("Connection URL   : " + cfg.getDbUrl());
        System.out.println("Database User    : " + cfg.getDbUser());
        System.out.println("Authentication   : " + cfg.getMaskedPassword());
        System.out.println("Require MySQL    : " + requireMySql);

        long runsCount = queryCount("SELECT COUNT(*) FROM runs");
        long agentRunsCount = queryCount("SELECT COUNT(*) FROM agent_runs");
        long activityCount = queryCount("SELECT COUNT(*) FROM user_activity");

        System.out.println("-".repeat(76));
        System.out.printf("Persistent Row Counts:\n");
        System.out.printf("  - runs          : %d rows\n", runsCount);
        System.out.printf("  - agent_runs    : %d rows\n", agentRunsCount);
        System.out.printf("  - user_activity : %d rows\n", activityCount);
        System.out.println("-".repeat(76));
        System.out.println("Exact SQL to execute in MySQL Workbench to verify records:");
        System.out.println("  USE pharoah_db;");
        System.out.println("  SELECT * FROM runs ORDER BY started_at DESC LIMIT 10;");
        System.out.println("  SELECT * FROM agent_runs ORDER BY created_at DESC LIMIT 20;");
        System.out.println("=".repeat(76) + "\n");
    }

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

    private long queryCount(String sql) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed counting rows for query (" + sql + "): " + e.getMessage());
        }
        return 0L;
    }
}
