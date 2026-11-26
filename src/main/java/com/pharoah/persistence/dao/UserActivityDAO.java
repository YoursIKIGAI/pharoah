package com.pharoah.persistence.dao;

import com.pharoah.core.model.UserActivity;
import com.pharoah.persistence.DatabaseConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// SANCHIT: Implemented UserActivityDAO.
// Handles JDBC persistence for user activity records in MySQL pharoah_db.
public class UserActivityDAO {

    private final DatabaseConnectionFactory connectionFactory;

    public UserActivityDAO(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    // SANCHIT: Saves user activity into MySQL table user_activity
    public void recordActivity(UserActivity activity) {
        String sql = """
            INSERT INTO user_activity (user_name, topic, workflow_type, sections_worked, agents_used, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, activity.getUserName());
            ps.setString(2, activity.getTopic());
            ps.setString(3, activity.getWorkflowType());
            ps.setString(4, activity.getSectionsWorked());
            ps.setString(5, activity.getAgentsUsed());
            ps.setString(6, activity.getStatus());
            ps.setTimestamp(7, Timestamp.valueOf(activity.getCreatedAt()));

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[JDBC WARN] Could not record activity to MySQL: " + e.getMessage());
        }
    }

    // SANCHIT: Retrieves all recorded activities for display
    public List<UserActivity> getAllActivities() {
        String sql = "SELECT * FROM user_activity ORDER BY created_at DESC";
        List<UserActivity> list = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new UserActivity(
                        rs.getInt("activity_id"),
                        rs.getString("user_name"),
                        rs.getString("topic"),
                        rs.getString("workflow_type"),
                        rs.getString("sections_worked"),
                        rs.getString("agents_used"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }

        } catch (SQLException e) {
            System.err.println("[JDBC WARN] Could not read user_activity from MySQL: " + e.getMessage());
        }

        return list;
    }
}
