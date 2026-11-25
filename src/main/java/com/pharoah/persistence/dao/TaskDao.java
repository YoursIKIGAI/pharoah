package com.pharoah.persistence.dao;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.persistence.DatabaseConnectionFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Task persistence using pure JDBC.
 * 
 * Demonstrates:
 * 1. PreparedStatement to prevent SQL Injection vulnerabilities.
 * 2. Parameter binding with typed setters (setString, setTimestamp).
 * 3. ResultSet navigation and typed mapping back to Java domain entities.
 * 4. ResultSetMetaData to inspect dynamic query schema at runtime.
 * 5. Transaction awareness by supporting externally managed Connection instances.
 */
public class TaskDao {

    private final DatabaseConnectionFactory connectionFactory;

    public TaskDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Inserts a new task using a PreparedStatement.
     * Can participate in an existing transaction if conn is provided.
     */
    public void insertTask(Task task, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO tasks (task_id, title, task_type, priority, status, input_payload, final_output, subject_id, created_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTaskId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getType().name());
            ps.setString(4, task.getPriority().name());
            ps.setString(5, task.getStatus().name());
            ps.setString(6, task.getInputPayload());
            ps.setString(7, task.getFinalOutput());
            ps.setString(8, task.getSubjectId());
            ps.setTimestamp(9, Timestamp.valueOf(task.getCreatedAt()));
            ps.setTimestamp(10, task.getCompletedAt() != null ? Timestamp.valueOf(task.getCompletedAt()) : null);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert task: Zero rows affected.");
            }
        }
    }

    public void insertTask(Task task) throws SQLException {
        try (Connection conn = connectionFactory.getConnection()) {
            insertTask(task, conn);
        }
    }

    /**
     * Updates an existing task's status and final output.
     */
    public void updateTask(Task task, Connection conn) throws SQLException {
        String sql = """
            UPDATE tasks 
            SET status = ?, final_output = ?, completed_at = ?
            WHERE task_id = ?;
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getStatus().name());
            ps.setString(2, task.getFinalOutput());
            ps.setTimestamp(3, task.getCompletedAt() != null ? Timestamp.valueOf(task.getCompletedAt()) : null);
            ps.setString(4, task.getTaskId());

            ps.executeUpdate();
        }
    }

    public void updateTask(Task task) throws SQLException {
        try (Connection conn = connectionFactory.getConnection()) {
            updateTask(task, conn);
        }
    }

    /**
     * Retrieves a task by its primary key ID.
     */
    public Task findById(String taskId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE task_id = ?;";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTask(rs);
                }
                return null;
            }
        }
    }

    /**
     * Retrieves all tasks and demonstrates ResultSetMetaData.
     */
    public List<Task> findAll() throws SQLException {
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC;";
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Educational demonstration of ResultSetMetaData
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            System.out.printf("[JDBC METADATA] Query returned %d columns:\n", columnCount);
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("  Col %d: %-15s (Type: %s)\n", i, rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
            }

            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
        }
        return tasks;
    }

    /**
     * Maps a single ResultSet row into a domain Task instance.
     */
    private Task mapRowToTask(ResultSet rs) throws SQLException {
        String id = rs.getString("task_id");
        String title = rs.getString("title");
        TaskType type = TaskType.valueOf(rs.getString("task_type"));
        Priority priority = Priority.valueOf(rs.getString("priority"));
        TaskStatus status = TaskStatus.valueOf(rs.getString("status"));
        String payload = rs.getString("input_payload");
        String output = rs.getString("final_output");
        String subjectId = rs.getString("subject_id");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Timestamp completedAtTs = rs.getTimestamp("completed_at");

        Task task = new Task(title, type, priority, payload, subjectId);
        // Note: For full fidelity in database hydration, status and outputs are updated
        task.updateStatus(status);
        if (output != null) {
            if (status == TaskStatus.COMPLETED) {
                task.markCompleted(output);
            } else if (status == TaskStatus.FAILED) {
                task.markFailed(output);
            }
        }
        return task;
    }
}
