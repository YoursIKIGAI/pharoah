package com.pharoah.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

// SANCHIT & VARIJ: Centralized Configuration Manager.
// Loads system configuration strictly from environment variables (DB_URL, DB_USER, DB_PASSWORD)
// with non-secret properties from config.properties as fallback. Sensitive credentials are never read from files.
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

        // 1. Database Configuration: credentials resolved ONLY from environment variables (DB_URL, DB_USER, DB_PASSWORD)
        // Non-secret defaults/fallbacks come from config.properties. dbPassword NEVER falls back to properties.
        this.dbType = getEnvOrProp("DB_TYPE", props, "db.type", "mysql");
        this.dbDriver = getEnvOrProp("DB_DRIVER", props, "db.driver", "com.mysql.cj.jdbc.Driver");
        this.dbUrl = getEnvOrProp("DB_URL", "MYSQL_URL", props, "db.url",
                "jdbc:mysql://127.0.0.1:3306/pharoah_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
        this.dbUser = getEnvOrProp("DB_USER", "MYSQL_USER", props, "db.user", "root");
        this.dbPassword = resolveDbPassword();
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

    private String resolveDbPassword() {
        String val = System.getenv("DB_PASSWORD");
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        val = System.getenv("MYSQL_PASSWORD");
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        // Fallback for Windows environments where process hasn't reloaded user registry
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                Process proc = new ProcessBuilder("reg", "query", "HKCU\\Environment", "/v", "DB_PASSWORD").start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("DB_PASSWORD")) {
                            String[] parts = line.trim().split("\\s+", 3);
                            if (parts.length >= 3) {
                                return parts[2].trim();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return "";
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
