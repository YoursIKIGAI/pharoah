package com.pharoah.persistence.dao;

import com.pharoah.persistence.DatabaseConnectionFactory;
import com.pharoah.project.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// ASHUTOSH: Implemented ProjectDao.
// Manages database persistence for Projects using PreparedStatement and ResultSet mapping.
public class ProjectDao {

    private final DatabaseConnectionFactory connectionFactory;

    public ProjectDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void create(Project project) throws SQLException {
        String sql = "INSERT INTO projects (project_id, name, description, status, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, project.getProjectId());
            ps.setString(2, project.getName());
            ps.setString(3, project.getDescription());
            ps.setString(4, project.getStatus());
            ps.setTimestamp(5, Timestamp.valueOf(project.getCreatedAt()));

            ps.executeUpdate();
        }
    }

    public Project findById(String projectId) throws SQLException {
        String sql = "SELECT * FROM projects WHERE project_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProject(rs);
                }
            }
        }
        return null;
    }

    public Project findByName(String name) throws SQLException {
        String sql = "SELECT * FROM projects WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProject(rs);
                }
            }
        }
        return null;
    }

    public List<Project> findAll() throws SQLException {
        String sql = "SELECT * FROM projects ORDER BY created_at DESC";
        List<Project> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToProject(rs));
            }
        }
        return list;
    }

    public void update(Project project) throws SQLException {
        String sql = "UPDATE projects SET name = ?, description = ?, status = ? WHERE project_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getStatus());
            ps.setString(4, project.getProjectId());

            ps.executeUpdate();
        }
    }

    public boolean delete(String projectId) throws SQLException {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectId);
            return ps.executeUpdate() > 0;
        }
    }

    private Project mapRowToProject(ResultSet rs) throws SQLException {
        return new Project(
                rs.getString("project_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
