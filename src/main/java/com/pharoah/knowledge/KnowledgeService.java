package com.pharoah.knowledge;

import com.pharoah.persistence.dao.KnowledgeDao;

import java.sql.SQLException;
import java.util.List;

// SANCHIT: Implemented KnowledgeService.
// Manages the persistent knowledge repository: URLs, notes, code snippets, research findings, and searches.
public class KnowledgeService {

    private final KnowledgeDao knowledgeDao;

    public KnowledgeService(KnowledgeDao knowledgeDao) {
        this.knowledgeDao = knowledgeDao;
    }

    public KnowledgeResource saveResource(String projectId, String title, ResourceType type, String contentOrUrl, String topicTag) throws SQLException {
        KnowledgeResource resource = new KnowledgeResource(projectId, title, type, contentOrUrl, topicTag);
        knowledgeDao.create(resource);
        return resource;
    }

    public List<KnowledgeResource> getResourcesByProject(String projectId) throws SQLException {
        return knowledgeDao.findByProject(projectId);
    }

    public List<KnowledgeResource> searchKnowledge(String keyword, String topicTag) throws SQLException {
        return knowledgeDao.search(keyword, topicTag);
    }

    public List<KnowledgeResource> getAllResources() throws SQLException {
        return knowledgeDao.findAll();
    }

    public boolean deleteResource(String resourceId) throws SQLException {
        return knowledgeDao.delete(resourceId);
    }
}
