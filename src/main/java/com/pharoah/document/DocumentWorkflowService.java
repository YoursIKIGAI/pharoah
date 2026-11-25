package com.pharoah.document;

import com.pharoah.persistence.dao.DocumentDao;

import java.sql.SQLException;
import java.util.List;

// SANCHIT & VARIJ: Implemented DocumentWorkflowService.
// Manages multi-section document lifecycles, section-level agent drafting, review, and final compilation.
public class DocumentWorkflowService {

    private final DocumentDao documentDao;

    public DocumentWorkflowService(DocumentDao documentDao) {
        this.documentDao = documentDao;
    }

    // SANCHIT: Creates a document and automatically generates its standard template sections
    public Document createDocumentWithTemplate(String projectId, String title, DocumentType type) throws SQLException {
        Document doc = new Document(projectId, title, type);
        documentDao.createDocument(doc);

        List<String> sectionNames = type.getStandardSections();
        for (int i = 0; i < sectionNames.size(); i++) {
            DocumentSection section = new DocumentSection(doc.getDocumentId(), sectionNames.get(i), i + 1);
            documentDao.createSection(section);
        }

        return doc;
    }

    public List<Document> getDocumentsByProject(String projectId) throws SQLException {
        return documentDao.findDocumentsByProject(projectId);
    }

    public Document getDocumentById(String documentId) throws SQLException {
        return documentDao.findDocumentById(documentId);
    }

    public List<DocumentSection> getSections(String documentId) throws SQLException {
        return documentDao.findSectionsByDocument(documentId);
    }

    public DocumentSection getSectionById(String sectionId) throws SQLException {
        return documentDao.findSectionById(sectionId);
    }

    // VARIJ: Assigns a specialized agent to draft or analyze a specific section
    public void assignAgentToSection(String sectionId, String agentId) throws SQLException {
        DocumentSection section = documentDao.findSectionById(sectionId);
        if (section != null) {
            section.setAssignedAgentId(agentId);
            documentDao.updateSection(section);
        }
    }

    // VARIJ: Updates section content with agent generated or edited draft
    public void updateSectionContent(String sectionId, String newContent) throws SQLException {
        DocumentSection section = documentDao.findSectionById(sectionId);
        if (section != null) {
            section.updateContent(newContent);
            section.setStatus("UNDER_REVIEW"); // Send to human review
            documentDao.updateSection(section);
        }
    }

    // SANCHIT: Human review approval of individual section draft
    public void reviewSection(String sectionId, boolean approved) throws SQLException {
        DocumentSection section = documentDao.findSectionById(sectionId);
        if (section != null) {
            section.setStatus(approved ? "APPROVED" : "REJECTED");
            documentDao.updateSection(section);
        }
    }

    // SANCHIT & VARIJ: Compiles all approved or drafted sections into a unified final document
    public String compileDocument(String documentId) throws SQLException {
        Document doc = documentDao.findDocumentById(documentId);
        if (doc == null) return "Document not found.";

        List<DocumentSection> sections = documentDao.findSectionsByDocument(documentId);
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(doc.getTitle()).append("\n");
        sb.append("**Format:** ").append(doc.getDocumentType().getDisplayName()).append("\n");
        sb.append("**Compiled via PHAROAH Multi-Agent Orchestrator**\n\n");
        sb.append("---\n\n");

        for (DocumentSection sec : sections) {
            sb.append("## ").append(sec.getSectionOrder()).append(". ").append(sec.getSectionName()).append("\n\n");
            if (sec.getContent() != null && !sec.getContent().trim().isEmpty()) {
                sb.append(sec.getContent().trim()).append("\n\n");
            } else {
                sb.append("*(Section not yet drafted)*\n\n");
            }
        }

        doc.setStatus("COMPILED");
        documentDao.updateDocument(doc);

        return sb.toString();
    }
}
