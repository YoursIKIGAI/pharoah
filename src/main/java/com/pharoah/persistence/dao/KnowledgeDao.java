package com.pharoah.persistence.dao;

import com.pharoah.knowledge.KnowledgeResource;
import com.pharoah.knowledge.ResourceType;
import com.pharoah.persistence.DatabaseConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// ASHUTOSH: Implemented KnowledgeDao.
// Manages relational persistence for knowledge assets (URLs, notes, snippets, references) with keyword search.
public class KnowledgeDao {

    private final DatabaseConnectionFactory connectionFactory;

    public KnowledgeDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void create(KnowledgeResource res) throws SQLException {
        String sql = "INSERT INTO knowledge_resources (resource_id, project_id, title, resource_type, content_or_url, topic_tag, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, res.getResourceId());
            ps.setString(2, res.getProjectId());
            ps.setString(3, res.getTitle());
            ps.setString(4, res.getResourceType().name());
            ps.setString(5, res.getContentOrUrl());
            ps.setString(6, res.getTopicTag());
            ps.setTimestamp(7, Timestamp.valueOf(res.getCreatedAt()));

            ps.executeUpdate();
        }
    }

    public KnowledgeResource findById(String resourceId) throws SQLException {
        String sql = "SELECT * FROM knowledge_resources WHERE resource_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToResource(rs);
                }
            }
        }
        return null;
    }

    public List<KnowledgeResource> findByProject(String projectId) throws SQLException {
        String sql = "SELECT * FROM knowledge_resources WHERE project_id = ? ORDER BY created_at DESC";
        List<KnowledgeResource> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToResource(rs));
                }
            }
        }
        return list;
    }

    public List<KnowledgeResource> search(String keyword, String topicTag) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM knowledge_resources WHERE 1=1 ");
        List<String> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(content_or_url) LIKE ?) ");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }

        if (topicTag != null && !topicTag.trim().isEmpty() && !topicTag.equalsIgnoreCase("ALL")) {
            sql.append("AND LOWER(topic_tag) = ? ");
            params.add(topicTag.trim().toLowerCase());
        }

        sql.append("ORDER BY created_at DESC");

        List<KnowledgeResource> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToResource(rs));
                }
            }
        }
        return list;
    }

    public List<KnowledgeResource> findAll() throws SQLException {
        String sql = "SELECT * FROM knowledge_resources ORDER BY created_at DESC";
        List<KnowledgeResource> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToResource(rs));
            }
        }
        return list;
    }

    public boolean delete(String resourceId) throws SQLException {
        String sql = "DELETE FROM knowledge_resources WHERE resource_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, resourceId);
            return ps.executeUpdate() > 0;
        }
    }

    private KnowledgeResource mapRowToResource(ResultSet rs) throws SQLException {
        return new KnowledgeResource(
                rs.getString("resource_id"),
                rs.getString("project_id"),
                rs.getString("title"),
                ResourceType.valueOf(rs.getString("resource_type")),
                rs.getString("content_or_url"),
                rs.getString("topic_tag"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
