package com.pharoah.persistence.dao;

import com.pharoah.core.model.Subject;
import com.pharoah.persistence.DatabaseConnectionFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Subject Knowledge Spaces.
 */
public class SubjectDao {

    private final DatabaseConnectionFactory connectionFactory;

    public SubjectDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void insertSubject(Subject subject, Connection conn) throws SQLException {
        String sql = "INSERT INTO subjects (subject_id, name, description, created_at) VALUES (?, ?, ?, ?);";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getSubjectId());
            ps.setString(2, subject.getName());
            ps.setString(3, subject.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(subject.getCreatedAt()));
            ps.executeUpdate();
        }
    }

    public void insertSubject(Subject subject) throws SQLException {
        try (Connection conn = connectionFactory.getConnection()) {
            insertSubject(subject, conn);
        }
    }

    public Subject findByName(String name) throws SQLException {
        String sql = "SELECT * FROM subjects WHERE LOWER(name) = LOWER(?);";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Subject(
                            rs.getString("subject_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
                return null;
            }
        }
    }

    public List<Subject> findAll() throws SQLException {
        String sql = "SELECT * FROM subjects ORDER BY name ASC;";
        List<Subject> subjects = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                subjects.add(new Subject(
                        rs.getString("subject_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        }
        return subjects;
    }
}
