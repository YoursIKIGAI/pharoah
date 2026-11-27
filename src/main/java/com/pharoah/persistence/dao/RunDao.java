package com.pharoah.persistence.dao;

import com.pharoah.orchestrator.AgentDeployment;
import com.pharoah.orchestrator.AgentOutput;
import com.pharoah.orchestrator.WorkflowPlan;
import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.persistence.TransactionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for persisting and querying master runs and individual agent runs.
 * Integrated with TransactionManager for atomic multi-table updates.
 */
public class RunDao {

    private final DatabaseConnectionFactory connectionFactory;
    private final TransactionManager transactionManager;

    public RunDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.transactionManager = new TransactionManager(connectionFactory);
    }

    public void persistRunAtomically(String runId,
                                    String prompt,
                                    String overallStatus,
                                    long startMs,
                                    long finishMs,
                                    WorkflowPlan plan,
                                    Map<String, AgentOutput> outputs,
                                    String modelName) throws SQLException {

        transactionManager.executeInTransaction(conn -> {
            // 1. Insert Master Run
            String runSql = connectionFactory.isMySqlActive()
                    ? "INSERT INTO runs (run_id, user_prompt, status, started_at, finished_at) VALUES (?, ?, ?, ?, ?)"
                    : "INSERT INTO runs (run_id, user_prompt, status, started_at, finished_at) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement runStmt = conn.prepareStatement(runSql)) {
                runStmt.setString(1, runId);
                runStmt.setString(2, prompt);
                runStmt.setString(3, overallStatus);
                runStmt.setTimestamp(4, new Timestamp(startMs));
                runStmt.setTimestamp(5, new Timestamp(finishMs));
                runStmt.executeUpdate();
            }

            // 2. Insert Individual Agent Runs
            String agentSql = connectionFactory.isMySqlActive()
                    ? """
                      INSERT INTO agent_runs (run_id, agent_name, role, model, thread_name, status, duration_ms, prompt_chars, output_chars, created_at)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                      """
                    : """
                      INSERT INTO agent_runs (run_id, agent_name, role, model, thread_name, status, duration_ms, prompt_chars, output_chars, created_at)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
                      """;

            try (PreparedStatement agentStmt = conn.prepareStatement(agentSql)) {
                for (AgentDeployment agent : plan.getAgents()) {
                    AgentOutput out = outputs.get(agent.getId());
                    String status = (out != null && out.isSuccess()) ? "COMPLETED" : "FAILED";
                    long duration = (out != null) ? out.getDurationMs() : 0L;
                    String threadName = (out != null) ? out.getThreadName() : agent.getAssignedThreadName();
                    int outChars = (out != null && out.getContent() != null) ? out.getContent().length() : 0;
                    int promptChars = agent.getResponsibility().length();

                    agentStmt.setString(1, runId);
                    agentStmt.setString(2, agent.getName());
                    agentStmt.setString(3, agent.getRole() != null ? agent.getRole() : agent.getKind().name());
                    agentStmt.setString(4, modelName);
                    agentStmt.setString(5, threadName != null ? threadName : "WorkerThread");
                    agentStmt.setString(6, status);
                    agentStmt.setLong(7, duration);
                    agentStmt.setInt(8, promptChars);
                    agentStmt.setInt(9, outChars);
                    agentStmt.addBatch();
                }
                agentStmt.executeBatch();
            }

            // 3. Insert Legacy user_activity record for backwards compatibility
            String actSql = connectionFactory.isMySqlActive()
                    ? "INSERT INTO user_activity (user_name, topic, workflow_type, sections_worked, agents_used, status, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)"
                    : "INSERT INTO user_activity (user_name, topic, workflow_type, sections_worked, agents_used, status, created_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'))";

            try (PreparedStatement actStmt = conn.prepareStatement(actSql)) {
                actStmt.setString(1, "User");
                actStmt.setString(2, prompt);
                actStmt.setString(3, "Prompt-Driven Swarm");
                actStmt.setString(4, String.valueOf(plan.getAgents().size()) + " Planned Sections");
                List<String> names = new ArrayList<>();
                for (AgentDeployment a : plan.getAgents()) names.add(a.getName());
                actStmt.setString(5, String.join(", ", names));
                actStmt.setString(6, overallStatus);
                actStmt.executeUpdate();
            }
        });
    }

    public long getRunsCount() {
        return queryCount("SELECT COUNT(*) FROM runs");
    }

    public long getAgentRunsCount() {
        return queryCount("SELECT COUNT(*) FROM agent_runs");
    }

    public long getUserActivityCount() {
        return queryCount("SELECT COUNT(*) FROM user_activity");
    }

    public List<String> getRecentRunSummaries(int limit) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT run_id, user_prompt, status, started_at FROM runs ORDER BY started_at DESC LIMIT " + Math.max(1, limit);
        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("run_id");
                String prompt = rs.getString("user_prompt");
                if (prompt.length() > 40) prompt = prompt.substring(0, 37) + "...";
                String status = rs.getString("status");
                Timestamp ts = rs.getTimestamp("started_at");
                list.add(String.format("[%s] Run: %s | Status: %s | Prompt: %s", ts, id, status, prompt));
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to query recent runs: " + e.getMessage());
        }
        return list;
    }

    private long queryCount(String sql) {
        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to execute count query: " + e.getMessage());
        }
        return 0;
    }
}
