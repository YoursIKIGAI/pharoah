package com.pharoah.web;

import com.pharoah.agents.Agent;
import com.pharoah.agents.impl.*;
import com.pharoah.ai.AIService;
import com.pharoah.ai.OfflineHeuristicAIService;
import com.pharoah.analytics.TaskAnalyticsEngine;
import com.pharoah.concurrency.ThreadPoolManager;
import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.knowledge.SubjectContextManager;
import com.pharoah.knowledge.SubjectMemory;
import com.pharoah.orchestrator.TaskOrchestrator;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.dao.SubjectDao;
import com.pharoah.persistence.dao.TaskDao;
import com.pharoah.tasks.TaskManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Built-in Embedded HTTP Web Server for PHAROAH.
 * 
 * Runs on http://localhost:8080/ using pure standard JDK HttpServer.
 * Exposes a rich, responsive Single-Page Dashboard and REST API endpoints.
 */
public class PharoahWebServer {

    private final int port;
    private final HttpServer server;
    private final TaskOrchestrator orchestrator;
    private final TaskManager taskManager;
    private final SubjectContextManager contextManager;
    private final TaskAnalyticsEngine analyticsEngine;
    private final ThreadPoolManager poolManager;

    private String activeSubject = "JAVA";

    public PharoahWebServer(int port) throws IOException {
        this.port = port;
        this.taskManager = new TaskManager();
        this.contextManager = new SubjectContextManager();
        this.analyticsEngine = new TaskAnalyticsEngine();
        this.poolManager = new ThreadPoolManager(Runtime.getRuntime().availableProcessors());

        DatabaseConnectionFactory dbFactory = new DatabaseConnectionFactory("jdbc:sqlite:pharoah.db");
        TaskDao taskDao = new TaskDao(dbFactory);
        SubjectDao subjectDao = new SubjectDao(dbFactory);

        this.orchestrator = new TaskOrchestrator(taskManager, poolManager, contextManager, taskDao, subjectDao);

        // Register the 10 specialized swarm agents
        AIService aiEngine = new OfflineHeuristicAIService();
        orchestrator.registerAgent(new WriterAgent(aiEngine));
        orchestrator.registerAgent(new ResearchAgent(aiEngine));
        orchestrator.registerAgent(new PaperReaderAgent(aiEngine));
        orchestrator.registerAgent(new SummariserAgent(aiEngine));
        orchestrator.registerAgent(new ComparatorAgent(aiEngine));
        orchestrator.registerAgent(new VerificationAgent(aiEngine));
        orchestrator.registerAgent(new DataAnalystAgent(aiEngine));
        orchestrator.registerAgent(new StudyAgent(aiEngine));
        orchestrator.registerAgent(new PlannerAgent(aiEngine));
        orchestrator.registerAgent(new DocumentAnalystAgent(aiEngine));

        // Bootstrap initial subject knowledge spaces
        contextManager.recordNote("JAVA", "Java 21 LTS: Records, Virtual Threads, Sequenced Collections.");
        contextManager.recordNote("JAVA", "ZGC Garbage Collector: Sub-millisecond pause times under load.");
        contextManager.recordNote("DBMS", "ACID: Atomicity, Consistency, Isolation, Durability.");
        contextManager.recordNote("DBMS", "B-Tree Indices: Balanced multi-way disk search in O(log N).");
        contextManager.recordNote("OPERATING SYSTEMS", "Virtual Memory: Paging, TLB caches, Page fault handling.");
        contextManager.recordNote("ARTIFICIAL INTELLIGENCE", "Transformer Multi-Head Attention Mechanisms.");

        // Create embedded HTTP Server
        this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
        setupRoutes();
        server.setExecutor(poolManager::submitTask);
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        PharoahWebServer webServer = new PharoahWebServer(port);
        webServer.start();

        System.out.println("=======================================================================");
        System.out.println("   👑 PHAROAH WEB SERVER IS ONLINE ON LOCALHOST!                      ");
        System.out.printf("   🌐 Open in browser: http://localhost:%d/                           \n", port);
        System.out.println("   Press Ctrl+C to stop the server.                                   ");
        System.out.println("=======================================================================");
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
        poolManager.gracefulShutdown(2);
    }

    private void setupRoutes() {
        // Root UI
        server.createContext("/", this::handleRoot);

        // REST API Endpoints
        server.createContext("/api/status", this::handleApiStatus);
        server.createContext("/api/agents", this::handleApiAgents);
        server.createContext("/api/subjects", this::handleApiSubjects);
        server.createContext("/api/tasks", this::handleApiTasks);
        server.createContext("/api/analytics", this::handleApiAnalytics);
        server.createContext("/api/notes", this::handleApiNotes);
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }

        String html = getDashboardHtml();
        sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
    }

    private void handleApiStatus(HttpExchange exchange) throws IOException {
        String json = String.format("""
            {
                "status": "ONLINE",
                "activeSubject": "%s",
                "registeredAgentsCount": %d,
                "workerPoolSize": %d,
                "totalTasksSubmitted": %d,
                "pendingQueueCount": %d
            }
            """,
                escapeJson(activeSubject),
                orchestrator.getRegisteredAgents().size(),
                poolManager.getPoolSize(),
                taskManager.getTotalSubmittedCount(),
                taskManager.getPendingCount());

        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleApiAgents(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        List<Agent> agents = orchestrator.getRegisteredAgents();
        for (int i = 0; i < agents.size(); i++) {
            Agent a = agents.get(i);
            sb.append(String.format("""
                {"id":"%s","name":"%s","description":"%s"}
                """, a.getAgentId(), escapeJson(a.getName()), escapeJson(a.getDescription())));
            if (i < agents.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendResponse(exchange, 200, sb.toString(), "application/json");
    }

    private void handleApiSubjects(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = readRequestBody(exchange);
            Map<String, String> params = parseFormOrJson(body);
            String newSubject = params.get("subject");
            if (newSubject != null && !newSubject.trim().isEmpty()) {
                this.activeSubject = newSubject.trim().toUpperCase();
                contextManager.getOrCreateMemory(this.activeSubject);
            }
            sendResponse(exchange, 200, "{\"success\":true,\"activeSubject\":\"" + escapeJson(activeSubject) + "\"}", "application/json");
            return;
        }

        Set<String> subjects = contextManager.getActiveSubjects();
        String jsonList = subjects.stream()
                .map(s -> "\"" + escapeJson(s) + "\"")
                .collect(Collectors.joining(","));
        String json = String.format("{\"activeSubject\":\"%s\",\"subjects\":[%s]}", escapeJson(activeSubject), jsonList);
        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleApiTasks(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = readRequestBody(exchange);
            Map<String, String> params = parseFormOrJson(body);

            String title = params.getOrDefault("title", "Untitled Task").trim();
            String typeStr = params.getOrDefault("type", "RESEARCH").trim().toUpperCase();
            String priorityStr = params.getOrDefault("priority", "MEDIUM").trim().toUpperCase();
            String payload = params.getOrDefault("payload", title).trim();
            String subject = params.getOrDefault("subject", activeSubject).trim().toUpperCase();

            TaskType taskType;
            try {
                taskType = TaskType.valueOf(typeStr);
            } catch (Exception e) {
                taskType = TaskType.RESEARCH;
            }

            Priority priority;
            try {
                priority = Priority.valueOf(priorityStr);
            } catch (Exception e) {
                priority = Priority.MEDIUM;
            }

            Task task = new Task(title, taskType, priority, payload, subject);

            try {
                Future<TaskResult> future = orchestrator.submitTaskAsync(task);
                TaskResult result = future.get(); // Asynchronously executed on worker pool

                String json = String.format("""
                    {
                        "taskId": "%s",
                        "status": "%s",
                        "agent": "%s",
                        "durationMs": %d,
                        "output": "%s"
                    }
                    """,
                        task.getTaskId(),
                        result.status(),
                        escapeJson(result.executedByAgent()),
                        result.executionDurationMs(),
                        escapeJson(result.output()));

                sendResponse(exchange, 200, json, "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
            return;
        }

        // GET: List all tasks
        List<Task> tasks = taskManager.getTaskHistory();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append(String.format("""
                {
                    "taskId": "%s",
                    "title": "%s",
                    "type": "%s",
                    "priority": "%s",
                    "status": "%s",
                    "subject": "%s",
                    "createdAt": "%s",
                    "output": "%s"
                }
                """,
                    t.getTaskId(),
                    escapeJson(t.getTitle()),
                    t.getType(),
                    t.getPriority(),
                    t.getStatus(),
                    escapeJson(t.getSubjectId()),
                    t.getCreatedAt(),
                    escapeJson(t.getFinalOutput() != null ? t.getFinalOutput() : "")));
            if (i < tasks.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendResponse(exchange, 200, sb.toString(), "application/json");
    }

    private void handleApiAnalytics(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getTaskHistory();

        Map<TaskType, Long> typeCounts = analyticsEngine.countTasksByType(tasks);
        Map<Boolean, List<Task>> partitioned = analyticsEngine.partitionByCompletion(tasks);
        DoubleSummaryStatistics stats = analyticsEngine.getPayloadLengthStatistics(tasks);

        String typeDistribution = typeCounts.entrySet().stream()
                .map(e -> String.format("\"%s\": %d", e.getKey().name(), e.getValue()))
                .collect(Collectors.joining(","));

        String json = String.format("""
            {
                "totalTasks": %d,
                "completedCount": %d,
                "pendingCount": %d,
                "categoryDistribution": {%s},
                "avgPayloadLength": %.2f,
                "distinctSubjects": "%s"
            }
            """,
                tasks.size(),
                partitioned.getOrDefault(true, List.of()).size(),
                partitioned.getOrDefault(false, List.of()).size(),
                typeDistribution,
                stats.getAverage(),
                escapeJson(analyticsEngine.getDistinctSubjectsJoined(tasks)));

        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleApiNotes(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = readRequestBody(exchange);
            Map<String, String> params = parseFormOrJson(body);
            String note = params.get("note");
            String subject = params.getOrDefault("subject", activeSubject).toUpperCase();

            if (note != null && !note.trim().isEmpty()) {
                contextManager.recordNote(subject, note.trim());
            }
            sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            return;
        }

        // GET
        SubjectMemory mem = contextManager.getOrCreateMemory(activeSubject);
        List<String> notes = mem.getNotes();
        String jsonNotes = notes.stream()
                .map(n -> "\"" + escapeJson(n) + "\"")
                .collect(Collectors.joining(","));

        sendResponse(exchange, 200, "{\"subject\":\"" + escapeJson(activeSubject) + "\",\"notes\":[" + jsonNotes + "]}", "application/json");
    }

    // ================= Helper Utilities =================

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             Scanner s = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private Map<String, String> parseFormOrJson(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple key-value JSON extractor
            String content = body.substring(1, body.length() - 1);
            String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].replaceAll("[\"\\s]", "");
                    String v = kv[1].trim().replaceAll("^\"|\"$", "").replace("\\\"", "\"");
                    map.put(k, v);
                }
            }
        } else {
            // Form URL-encoded
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText, String contentType) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ================= Embedded Dashboard HTML =================

    private String getDashboardHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>👑 PHAROAH — Multi-Agent Intelligence & Knowledge System</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Fira+Code:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-base: #0B0F19;
            --bg-surface: #111827;
            --bg-card: #1F2937;
            --bg-card-hover: #374151;
            --primary: #6366F1;
            --primary-hover: #4F46E5;
            --primary-glow: rgba(99, 102, 241, 0.25);
            --accent-gold: #F59E0B;
            --text-main: #F9FAFB;
            --text-muted: #9CA3AF;
            --border: #374151;
            --success: #10B981;
            --warning: #F59E0B;
            --danger: #EF4444;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--bg-base);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* Top Navbar */
        header {
            background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border);
            padding: 1rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 50;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            font-size: 1.35rem;
            font-weight: 800;
            letter-spacing: -0.5px;
            color: #fff;
        }

        .brand span.crown {
            color: var(--accent-gold);
            font-size: 1.5rem;
        }

        .badge-live {
            background: rgba(16, 185, 129, 0.15);
            color: var(--success);
            border: 1px solid rgba(16, 185, 129, 0.3);
            font-size: 0.75rem;
            font-weight: 600;
            padding: 0.2rem 0.6rem;
            border-radius: 9999px;
            display: flex;
            align-items: center;
            gap: 0.35rem;
        }

        .badge-live::before {
            content: '';
            width: 6px;
            height: 6px;
            background-color: var(--success);
            border-radius: 50%;
            display: inline-block;
            box-shadow: 0 0 8px var(--success);
        }

        /* Subject Workspace Selector */
        .workspace-bar {
            background: var(--bg-surface);
            border-bottom: 1px solid var(--border);
            padding: 0.75rem 2rem;
            display: flex;
            align-items: center;
            gap: 1rem;
            overflow-x: auto;
        }

        .ws-label {
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .pill-group {
            display: flex;
            gap: 0.5rem;
        }

        .pill {
            background: var(--bg-card);
            color: var(--text-muted);
            border: 1px solid var(--border);
            padding: 0.35rem 0.85rem;
            border-radius: 8px;
            font-size: 0.85rem;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .pill:hover {
            border-color: var(--primary);
            color: #fff;
        }

        .pill.active {
            background: var(--primary);
            color: #fff;
            border-color: var(--primary);
            box-shadow: 0 0 12px var(--primary-glow);
        }

        .btn-new-ws {
            background: transparent;
            color: var(--accent-gold);
            border: 1px dashed var(--accent-gold);
            padding: 0.35rem 0.75rem;
            border-radius: 8px;
            font-size: 0.85rem;
            cursor: pointer;
            transition: all 0.2s;
        }

        .btn-new-ws:hover {
            background: rgba(245, 158, 11, 0.1);
        }

        /* Main Container */
        .main-layout {
            display: grid;
            grid-template-columns: 1.15fr 0.85fr;
            gap: 1.5rem;
            padding: 1.75rem 2rem;
            flex: 1;
        }

        /* Cards */
        .card {
            background-color: var(--bg-surface);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1.5rem;
            display: flex;
            flex-direction: column;
            gap: 1.25rem;
        }

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid var(--border);
            padding-bottom: 0.85rem;
        }

        .card-title {
            font-size: 1.1rem;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        /* Form Controls */
        .form-group {
            display: flex;
            flex-direction: column;
            gap: 0.45rem;
        }

        label {
            font-size: 0.825rem;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        input[type="text"], select, textarea {
            background-color: var(--bg-card);
            border: 1px solid var(--border);
            color: var(--text-main);
            padding: 0.75rem 1rem;
            border-radius: 8px;
            font-size: 0.95rem;
            font-family: inherit;
            outline: none;
            transition: border-color 0.2s;
        }

        input[type="text"]:focus, select:focus, textarea:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--primary-glow);
        }

        textarea {
            resize: vertical;
            min-height: 110px;
        }

        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 1rem;
        }

        .btn-primary {
            background: linear-gradient(135deg, var(--primary) 0%, var(--primary-hover) 100%);
            color: white;
            border: none;
            padding: 0.85rem 1.5rem;
            border-radius: 8px;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            transition: transform 0.15s, box-shadow 0.15s;
            box-shadow: 0 4px 14px var(--primary-glow);
        }

        .btn-primary:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 20px var(--primary-glow);
        }

        .btn-primary:active {
            transform: translateY(0);
        }

        /* Execution Result Box */
        .result-box {
            background-color: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 1.25rem;
            min-height: 140px;
            font-family: 'Inter', sans-serif;
            font-size: 0.95rem;
            line-height: 1.6;
            white-space: pre-wrap;
            color: #E5E7EB;
            overflow-y: auto;
            max-height: 380px;
        }

        /* Right Column Tabs */
        .tabs {
            display: flex;
            gap: 0.5rem;
            border-bottom: 1px solid var(--border);
            padding-bottom: 0.5rem;
        }

        .tab-btn {
            background: transparent;
            color: var(--text-muted);
            border: none;
            font-size: 0.85rem;
            font-weight: 600;
            padding: 0.45rem 0.85rem;
            cursor: pointer;
            border-radius: 6px;
            transition: all 0.2s;
        }

        .tab-btn.active {
            background: var(--bg-card);
            color: var(--primary);
        }

        .tab-pane {
            display: none;
            flex-direction: column;
            gap: 1rem;
        }

        .tab-pane.active {
            display: flex;
        }

        /* Notes & Agent Cards */
        .item-list {
            display: flex;
            flex-direction: column;
            gap: 0.65rem;
            max-height: 380px;
            overflow-y: auto;
        }

        .note-card, .agent-card, .task-card {
            background: var(--bg-card);
            border: 1px solid var(--border);
            padding: 0.75rem 1rem;
            border-radius: 8px;
            font-size: 0.875rem;
        }

        .agent-card {
            display: flex;
            flex-direction: column;
            gap: 0.25rem;
        }

        .agent-title {
            font-weight: 600;
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .agent-desc {
            font-size: 0.8rem;
            color: var(--text-muted);
        }

        /* Analytics stats grid */
        .stat-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 0.85rem;
        }

        .stat-box {
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 0.85rem;
            display: flex;
            flex-direction: column;
            gap: 0.35rem;
        }

        .stat-val {
            font-size: 1.5rem;
            font-weight: 700;
            color: #fff;
        }

        .stat-lbl {
            font-size: 0.75rem;
            color: var(--text-muted);
            text-transform: uppercase;
        }

        /* Footer */
        footer {
            border-top: 1px solid var(--border);
            background: var(--bg-surface);
            padding: 1rem 2rem;
            font-size: 0.8rem;
            color: var(--text-muted);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        @media (max-width: 900px) {
            .main-layout {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>

    <!-- Header -->
    <header>
        <div class="brand">
            <span class="crown">👑</span> PHAROAH
            <span style="font-weight: 400; font-size: 0.95rem; color: var(--text-muted);">| Task Intelligence & Knowledge System</span>
        </div>
        <div style="display: flex; align-items: center; gap: 1rem;">
            <div class="badge-live">Java 21 LTS Server Online</div>
            <span style="font-size: 0.85rem; color: var(--text-muted);">Port: 8080</span>
        </div>
    </header>

    <!-- Workspace Selector Bar -->
    <div class="workspace-bar">
        <span class="ws-label">Workspace:</span>
        <div class="pill-group" id="subjectPills">
            <!-- Dynamically populated -->
            <button class="pill active">JAVA</button>
            <button class="pill">DBMS</button>
            <button class="pill">OPERATING SYSTEMS</button>
        </div>
        <button class="btn-new-ws" onclick="promptNewSubject()">+ New Workspace</button>
    </div>

    <!-- Main Content -->
    <div class="main-layout">
        
        <!-- Left Column: Task Submission & Swarm Output -->
        <div style="display: flex; flex-direction: column; gap: 1.5rem;">
            
            <div class="card">
                <div class="card-header">
                    <span class="card-title">⚡ Dispatch Multi-Agent Swarm</span>
                    <span style="font-size: 0.8rem; color: var(--text-muted);" id="activeWsIndicator">Target: JAVA</span>
                </div>

                <div class="form-group">
                    <label>Task Title / Intellectual Goal</label>
                    <input type="text" id="taskTitle" placeholder="e.g. Compare QuickSort vs MergeSort, or Explain Deadlock Prevention">
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label>Task Category</label>
                        <select id="taskCategory">
                            <option value="RESEARCH">🔬 Topic Research & Paper Dissection</option>
                            <option value="COMPARISON" selected>⚖️ Technology & Concept Comparison</option>
                            <option value="STUDY_ASSISTANCE">🎓 Study Aid, Revision Notes & MCQs</option>
                            <option value="WRITING">✍️ Technical Writing & Report Drafting</option>
                            <option value="VERIFICATION">🛡️ Fact & Claim Verification</option>
                            <option value="DATA_ANALYSIS">📊 Dataset & CSV Statistical Analysis</option>
                            <option value="PLANNING">📅 Milestone Planning & Task Breakdown</option>
                            <option value="DOCUMENT_ANALYSIS">📄 Deep Document Intelligence</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Execution Priority</label>
                        <select id="taskPriority">
                            <option value="HIGH">🔴 High (PriorityQueue Precedence)</option>
                            <option value="MEDIUM" selected>🟡 Medium (Standard Scheduling)</option>
                            <option value="LOW">🟢 Low (Background Queue)</option>
                        </select>
                    </div>
                </div>

                <div class="form-group">
                    <label>Detailed Instructions / Document Text</label>
                    <textarea id="taskPayload" placeholder="Provide input query, code snippet, dataset, or notes..."></textarea>
                </div>

                <button class="btn-primary" onclick="submitTask()" id="submitBtn">
                    <span>🚀 Deploy Swarm Agent</span>
                </button>
            </div>

            <!-- Agent Output Card -->
            <div class="card">
                <div class="card-header">
                    <span class="card-title">📄 Agent Synthesized Intelligence Output</span>
                    <span style="font-size: 0.8rem; color: var(--accent-gold);" id="executionBadge">Status: Ready</span>
                </div>
                <div class="result-box" id="outputBox">Submit an intellectual task above to see specialized agent analysis here...</div>
            </div>

        </div>

        <!-- Right Column: Tabs (Knowledge Space Notes, Swarm Agents, Stream Analytics, History) -->
        <div class="card">
            
            <div class="tabs">
                <button class="tab-btn active" onclick="switchTab('tab-notes')">📚 Notes</button>
                <button class="tab-btn" onclick="switchTab('tab-agents')">👥 Swarm (10)</button>
                <button class="tab-btn" onclick="switchTab('tab-analytics')">📊 Streams</button>
                <button class="tab-btn" onclick="switchTab('tab-history')">📜 History</button>
            </div>

            <!-- Tab 1: Subject Notes -->
            <div class="tab-pane active" id="tab-notes">
                <div style="display: flex; gap: 0.5rem;">
                    <input type="text" id="newNoteInput" placeholder="Add note to current subject..." style="flex:1;">
                    <button class="pill active" onclick="addNote()">+ Add</button>
                </div>
                <div class="item-list" id="notesList">
                    <!-- Populated via API -->
                </div>
            </div>

            <!-- Tab 2: 10 Swarm Agents -->
            <div class="tab-pane" id="tab-agents">
                <div class="item-list" id="agentsList">
                    <!-- Populated via API -->
                </div>
            </div>

            <!-- Tab 3: Stream API Analytics -->
            <div class="tab-pane" id="tab-analytics">
                <div class="stat-grid" id="analyticsGrid">
                    <div class="stat-box">
                        <span class="stat-val" id="statTotalTasks">0</span>
                        <span class="stat-lbl">Total Tasks</span>
                    </div>
                    <div class="stat-box">
                        <span class="stat-val" id="statCompletedTasks">0</span>
                        <span class="stat-lbl">Completed</span>
                    </div>
                    <div class="stat-box">
                        <span class="stat-val" id="statAvgChars">0</span>
                        <span class="stat-lbl">Avg Characters</span>
                    </div>
                    <div class="stat-box">
                        <span class="stat-val" id="statActiveSubjects">0</span>
                        <span class="stat-lbl">Knowledge Spaces</span>
                    </div>
                </div>
                <div style="margin-top: 1rem;">
                    <label>Category Distribution (Collectors.groupingBy):</label>
                    <div class="result-box" id="categoryDistributionBox" style="min-height: 120px; font-size: 0.85rem;"></div>
                </div>
            </div>

            <!-- Tab 4: History -->
            <div class="tab-pane" id="tab-history">
                <div class="item-list" id="historyList">
                    <!-- Populated via API -->
                </div>
            </div>

        </div>

    </div>

    <!-- Footer -->
    <footer>
        <div>PHAROAH System — Concurrency & Knowledge Spaces Engine</div>
        <div>Demonstrating Java 21: OOP, Collections, Multithreading, JDBC, Streams & Generics</div>
    </footer>

    <script>
        let currentSubject = "JAVA";

        async function loadSubjects() {
            try {
                const res = await fetch('/api/subjects');
                const data = await res.json();
                currentSubject = data.activeSubject;
                document.getElementById('activeWsIndicator').innerText = "Target: " + currentSubject;

                const pillsContainer = document.getElementById('subjectPills');
                pillsContainer.innerHTML = '';
                data.subjects.forEach(subj => {
                    const btn = document.createElement('button');
                    btn.className = 'pill' + (subj === currentSubject ? ' active' : '');
                    btn.innerText = subj;
                    btn.onclick = () => selectSubject(subj);
                    pillsContainer.appendChild(btn);
                });

                loadNotes();
            } catch (err) {
                console.error("Failed to load subjects:", err);
            }
        }

        async function selectSubject(subj) {
            await fetch('/api/subjects', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({subject: subj})
            });
            loadSubjects();
        }

        async function promptNewSubject() {
            const name = prompt("Enter new subject workspace name (e.g. CYBERSECURITY, AI):");
            if (name && name.trim()) {
                await selectSubject(name.trim().toUpperCase());
            }
        }

        async function loadNotes() {
            try {
                const res = await fetch('/api/notes');
                const data = await res.json();
                const list = document.getElementById('notesList');
                list.innerHTML = '';
                if (data.notes.length === 0) {
                    list.innerHTML = '<div style="color:var(--text-muted);font-size:0.85rem;">No notes yet in ' + currentSubject + '.</div>';
                    return;
                }
                data.notes.forEach(note => {
                    const div = document.createElement('div');
                    div.className = 'note-card';
                    div.innerText = note;
                    list.appendChild(div);
                });
            } catch (err) {
                console.error("Failed to load notes:", err);
            }
        }

        async function addNote() {
            const input = document.getElementById('newNoteInput');
            const note = input.value.trim();
            if (!note) return;

            await fetch('/api/notes', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({note: note, subject: currentSubject})
            });
            input.value = '';
            loadNotes();
        }

        async function loadAgents() {
            try {
                const res = await fetch('/api/agents');
                const agents = await res.json();
                const list = document.getElementById('agentsList');
                list.innerHTML = '';
                agents.forEach(a => {
                    const div = document.createElement('div');
                    div.className = 'agent-card';
                    div.innerHTML = `
                        <div class="agent-title"><span>${a.name}</span><span style="font-size:0.75rem;color:var(--success);">ACTIVE</span></div>
                        <div class="agent-desc">${a.description}</div>
                    `;
                    list.appendChild(div);
                });
            } catch (err) {
                console.error("Failed to load agents:", err);
            }
        }

        async function loadAnalytics() {
            try {
                const res = await fetch('/api/analytics');
                const data = await res.json();
                document.getElementById('statTotalTasks').innerText = data.totalTasks;
                document.getElementById('statCompletedTasks').innerText = data.completedCount;
                document.getElementById('statAvgChars').innerText = Math.round(data.avgPayloadLength || 0);
                document.getElementById('statActiveSubjects').innerText = data.distinctSubjects ? data.distinctSubjects.split(',').length : 0;

                let distHtml = '';
                for (let [cat, count] of Object.entries(data.categoryDistribution || {})) {
                    distHtml += `${cat.padEnd(22)} : ${count} tasks\\n`;
                }
                document.getElementById('categoryDistributionBox').innerText = distHtml || 'No task distribution yet.';
            } catch (err) {
                console.error("Failed to load analytics:", err);
            }
        }

        async function loadHistory() {
            try {
                const res = await fetch('/api/tasks');
                const tasks = await res.json();
                const list = document.getElementById('historyList');
                list.innerHTML = '';
                if (tasks.length === 0) {
                    list.innerHTML = '<div style="color:var(--text-muted);font-size:0.85rem;">No tasks executed yet.</div>';
                    return;
                }
                tasks.forEach(t => {
                    const div = document.createElement('div');
                    div.className = 'task-card';
                    div.innerHTML = `
                        <div style="font-weight:600;display:flex;justify-content:space-between;">
                            <span>${t.title}</span>
                            <span style="color:var(--primary);font-size:0.75rem;">${t.priority}</span>
                        </div>
                        <div style="font-size:0.75rem;color:var(--text-muted);margin-top:0.25rem;">
                            Category: ${t.type} | Subject: ${t.subject} | Status: ${t.status}
                        </div>
                    `;
                    list.appendChild(div);
                });
            } catch (err) {
                console.error("Failed to load history:", err);
            }
        }

        async function submitTask() {
            const title = document.getElementById('taskTitle').value.trim();
            const category = document.getElementById('taskCategory').value;
            const priority = document.getElementById('taskPriority').value;
            const payload = document.getElementById('taskPayload').value.trim() || title;

            if (!title) {
                alert("Please enter a task title!");
                return;
            }

            const btn = document.getElementById('submitBtn');
            const badge = document.getElementById('executionBadge');
            const outBox = document.getElementById('outputBox');

            btn.disabled = true;
            btn.innerHTML = '<span>⏳ Agent Swarm Executing Concurrently...</span>';
            badge.innerText = 'Status: In-Flight';
            outBox.innerText = 'Task submitted to PriorityQueue. Worker thread picking up task...';

            try {
                const res = await fetch('/api/tasks', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        title: title,
                        type: category,
                        priority: priority,
                        payload: payload,
                        subject: currentSubject
                    })
                });

                const data = await res.json();
                badge.innerText = `Agent: ${data.agent} (${data.durationMs} ms)`;
                outBox.innerText = data.output;

                loadAnalytics();
                loadHistory();
                loadNotes();

            } catch (err) {
                badge.innerText = 'Status: Failed';
                outBox.innerText = 'Error: ' + err.message;
            } finally {
                btn.disabled = false;
                btn.innerHTML = '<span>🚀 Deploy Swarm Agent</span>';
            }
        }

        function switchTab(tabId) {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

            event.target.classList.add('active');
            document.getElementById(tabId).classList.add('active');

            if (tabId === 'tab-analytics') loadAnalytics();
            if (tabId === 'tab-history') loadHistory();
            if (tabId === 'tab-notes') loadNotes();
            if (tabId === 'tab-agents') loadAgents();
        }

        // Initialize on page load
        window.onload = () => {
            loadSubjects();
            loadAgents();
            loadAnalytics();
            loadHistory();
        };
    </script>
</body>
</html>
""";
    }
}
