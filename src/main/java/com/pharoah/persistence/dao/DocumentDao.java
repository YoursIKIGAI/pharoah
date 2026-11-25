package com.pharoah.persistence.dao;

import com.pharoah.document.Document;
import com.pharoah.document.DocumentSection;
import com.pharoah.document.DocumentType;
import com.pharoah.persistence.DatabaseConnectionFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ASHUTOSH: Implemented DocumentDao.
// Manages relational persistence for multi-section Documents and individual DocumentSections.
public class DocumentDao {

    private final DatabaseConnectionFactory connectionFactory;

    public DocumentDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    // ================= Document Operations =================

    public void createDocument(Document doc) throws SQLException {
        String sql = "INSERT INTO documents (document_id, project_id, title, document_type, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doc.getDocumentId());
            ps.setString(2, doc.getProjectId());
            ps.setString(3, doc.getTitle());
            ps.setString(4, doc.getDocumentType().name());
            ps.setString(5, doc.getStatus());
            ps.setTimestamp(6, Timestamp.valueOf(doc.getCreatedAt()));

            ps.executeUpdate();
        }
    }

    public Document findDocumentById(String documentId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE document_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Document(
                            rs.getString("document_id"),
                            rs.getString("project_id"),
                            rs.getString("title"),
                            DocumentType.valueOf(rs.getString("document_type")),
                            rs.getString("status"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        }
        return null;
    }

    public List<Document> findDocumentsByProject(String projectId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE project_id = ? ORDER BY created_at DESC";
        List<Document> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Document(
                            rs.getString("document_id"),
                            rs.getString("project_id"),
                            rs.getString("title"),
                            DocumentType.valueOf(rs.getString("document_type")),
                            rs.getString("status"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return list;
    }

    public void updateDocument(Document doc) throws SQLException {
        String sql = "UPDATE documents SET title = ?, status = ? WHERE document_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doc.getTitle());
            ps.setString(2, doc.getStatus());
            ps.setString(3, doc.getDocumentId());

            ps.executeUpdate();
        }
    }

    public boolean deleteDocument(String documentId) throws SQLException {
        String sql = "DELETE FROM documents WHERE document_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, documentId);
            return ps.executeUpdate() > 0;
        }
    }

    // ================= Section Operations =================

    public void createSection(DocumentSection section) throws SQLException {
        String sql = "INSERT INTO document_sections (section_id, document_id, section_name, section_order, assigned_agent_id, content, status, version, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, section.getSectionId());
            ps.setString(2, section.getDocumentId());
            ps.setString(3, section.getSectionName());
            ps.setInt(4, section.getSectionOrder());
            ps.setString(5, section.getAssignedAgentId());
            ps.setString(6, section.getContent());
            ps.setString(7, section.getStatus());
            ps.setInt(8, section.getVersion());
            ps.setTimestamp(9, Timestamp.valueOf(section.getUpdatedAt()));

            ps.executeUpdate();
        }
    }

    public void updateSection(DocumentSection section) throws SQLException {
        String sql = "UPDATE document_sections SET assigned_agent_id = ?, content = ?, status = ?, version = ?, updated_at = ? WHERE section_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, section.getAssignedAgentId());
            ps.setString(2, section.getContent());
            ps.setString(3, section.getStatus());
            ps.setInt(4, section.getVersion());
            ps.setTimestamp(5, Timestamp.valueOf(section.getUpdatedAt()));
            ps.setString(6, section.getSectionId());

            ps.executeUpdate();
        }
    }

    public List<DocumentSection> findSectionsByDocument(String documentId) throws SQLException {
        String sql = "SELECT * FROM document_sections WHERE document_id = ? ORDER BY section_order ASC";
        List<DocumentSection> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DocumentSection(
                            rs.getString("section_id"),
                            rs.getString("document_id"),
                            rs.getString("section_name"),
                            rs.getInt("section_order"),
                            rs.getString("assigned_agent_id"),
                            rs.getString("content"),
                            rs.getString("status"),
                            rs.getInt("version"),
                            rs.getTimestamp("updated_at").toLocalDateTime()
                    ));
                }
            }
        }
        return list;
    }

    public DocumentSection findSectionById(String sectionId) throws SQLException {
        String sql = "SELECT * FROM document_sections WHERE section_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DocumentSection(
                            rs.getString("section_id"),
                            rs.getString("document_id"),
                            rs.getString("section_name"),
                            rs.getInt("section_order"),
                            rs.getString("assigned_agent_id"),
                            rs.getString("content"),
                            rs.getString("status"),
                            rs.getInt("version"),
                            rs.getTimestamp("updated_at").toLocalDateTime()
                    );
                }
            }
        }
        return null;
    }
}
