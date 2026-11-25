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

// ASHUTOSH: Implemented TaskDao.
// Manages relational persistence for Tasks with project linkage, assigned agents, and dependencies.
public class TaskDao {

    private final DatabaseConnectionFactory connectionFactory;

    public TaskDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertTask(Task task, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO tasks (task_id, project_id, title, task_type, priority, status, input_payload,
                               assigned_agent_id, depends_on_task_id, final_output, review_notes, created_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTaskId());
            ps.setString(2, task.getProjectId());
            ps.setString(3, task.getTitle());
            ps.setString(4, task.getType().name());
            ps.setString(5, task.getPriority().name());
            ps.setString(6, task.getStatus().name());
            ps.setString(7, task.getInputPayload());
            ps.setString(8, task.getAssignedAgentId());
            ps.setString(9, task.getDependsOnTaskId());
            ps.setString(10, task.getFinalOutput());
            ps.setString(11, task.getReviewNotes());
            ps.setTimestamp(12, Timestamp.valueOf(task.getCreatedAt()));
            ps.setTimestamp(13, task.getCompletedAt() != null ? Timestamp.valueOf(task.getCompletedAt()) : null);

            ps.executeUpdate();
        }
    }

    public void insertTask(Task task) throws SQLException {
        try (Connection conn = connectionFactory.getConnection()) {
            insertTask(task, conn);
        }
    }

    public void updateTask(Task task, Connection conn) throws SQLException {
        String sql = """
            UPDATE tasks
            SET status = ?, final_output = ?, review_notes = ?, assigned_agent_id = ?, completed_at = ?
            WHERE task_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getStatus().name());
            ps.setString(2, task.getFinalOutput());
            ps.setString(3, task.getReviewNotes());
            ps.setString(4, task.getAssignedAgentId());
            ps.setTimestamp(5, task.getCompletedAt() != null ? Timestamp.valueOf(task.getCompletedAt()) : null);
            ps.setString(6, task.getTaskId());

            ps.executeUpdate();
        }
    }

    public void updateTask(Task task) throws SQLException {
        try (Connection conn = connectionFactory.getConnection()) {
            updateTask(task, conn);
        }
    }

    public Task findById(String taskId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE task_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTask(rs);
                }
            }
        }
        return null;
    }

    public List<Task> findByProject(String projectId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE project_id = ? ORDER BY created_at ASC";
        List<Task> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        }
        return list;
    }

    public List<Task> findAll() throws SQLException {
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC";
        List<Task> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToTask(rs));
            }
        }
        return list;
    }

    public boolean delete(String taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, taskId);
            return ps.executeUpdate() > 0;
        }
    }

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        Timestamp compTs = rs.getTimestamp("completed_at");
        return new Task(
                rs.getString("task_id"),
                rs.getString("project_id"),
                rs.getString("title"),
                TaskType.valueOf(rs.getString("task_type")),
                Priority.valueOf(rs.getString("priority")),
                rs.getString("input_payload"),
                rs.getString("assigned_agent_id"),
                rs.getString("depends_on_task_id"),
                TaskStatus.valueOf(rs.getString("status")),
                rs.getString("final_output"),
                rs.getString("review_notes"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                compTs != null ? compTs.toLocalDateTime() : null
        );
    }
}
