package com.pharoah.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

// SANCHIT & VARIJ: Centralized Configuration Manager.
// Loads system configuration from config.properties, .env, or environment variables.
// Eliminates scattered configuration values and hardcoded credentials across the codebase.
public class AppConfig {

    private static volatile AppConfig instance;

    private final String dbType;
    private final String dbDriver;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String dbFallbackSqlite;
    private final String pythonCommand;
    private final boolean ansiColorsEnabled;
    private final String aiProvider;
    private final String aiEndpoint;

    private AppConfig() {
        Properties props = loadProperties();

        // 1. Database Configuration with Environment Variable Precedence
        this.dbType = getEnvOrProp("DB_TYPE", props, "db.type", "mysql");
        this.dbDriver = getEnvOrProp("DB_DRIVER", props, "db.driver", "com.mysql.cj.jdbc.Driver");
        this.dbUrl = getEnvOrProp("DB_URL", "MYSQL_URL", props, "db.url",
                "jdbc:mysql://localhost:3306/pharoah_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
        this.dbUser = getEnvOrProp("DB_USER", "MYSQL_USER", props, "db.user", "root");
        this.dbPassword = getEnvOrProp("DB_PASSWORD", "MYSQL_PASSWORD", props, "db.password", "");
        this.dbFallbackSqlite = getEnvOrProp("DB_FALLBACK", props, "db.fallback.sqlite", "jdbc:sqlite:pharoah.db");

        // 2. Python Runtime Configuration
        this.pythonCommand = getEnvOrProp("PYTHON_CMD", "PYTHON_PATH", props, "python.command", "python");

        // 3. UI Styling Configuration
        String ansiVal = getEnvOrProp("UI_ANSI", props, "ui.ansi.colors", "true");
        this.ansiColorsEnabled = Boolean.parseBoolean(ansiVal);

        // 4. Pluggable AI Configuration
        this.aiProvider = getEnvOrProp("AI_PROVIDER", props, "ai.provider", "MOCK_HEURISTIC");
        this.aiEndpoint = getEnvOrProp("AI_ENDPOINT", props, "ai.endpoint", "http://localhost:11434/api/generate");
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    // Loads properties from config.properties (preferred) or db.properties (fallback)
    private Properties loadProperties() {
        Properties props = new Properties();

        // Try config.properties on disk
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (InputStream is = new FileInputStream(configFile)) {
                props.load(is);
                return props;
            } catch (Exception ignored) {}
        }

        // Try db.properties on disk (backwards compatibility)
        File dbPropFile = new File("db.properties");
        if (dbPropFile.exists()) {
            try (InputStream is = new FileInputStream(dbPropFile)) {
                props.load(is);
                return props;
            } catch (Exception ignored) {}
        }

        // Try classpath resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {}

        return props;
    }

    private String getEnvOrProp(String envKey, Properties props, String propKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        return props.getProperty(propKey, defaultValue).trim();
    }

    private String getEnvOrProp(String envKey1, String envKey2, Properties props, String propKey, String defaultValue) {
        String envVal1 = System.getenv(envKey1);
        if (envVal1 != null && !envVal1.isBlank()) return envVal1.trim();
        String envVal2 = System.getenv(envKey2);
        if (envVal2 != null && !envVal2.isBlank()) return envVal2.trim();
        return props.getProperty(propKey, defaultValue).trim();
    }

    // --- Public Getters ---

    public String getDbType() {
        return dbType;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbFallbackSqlite() {
        return dbFallbackSqlite;
    }

    public String getPythonCommand() {
        return pythonCommand;
    }

    public boolean isAnsiColorsEnabled() {
        return ansiColorsEnabled;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiEndpoint() {
        return aiEndpoint;
    }

    public String getMaskedPassword() {
        if (dbPassword == null || dbPassword.isEmpty()) return "[NOT SET]";
        return "[CONFIGURED: " + "*".repeat(Math.min(dbPassword.length(), 8)) + "]";
    }
}
