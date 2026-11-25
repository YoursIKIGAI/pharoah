package com.pharoah.project;

import com.pharoah.persistence.dao.ProjectDao;

import java.sql.SQLException;
import java.util.List;

// SANCHIT: Implemented ProjectService.
// Manages business logic for project workspaces, current active project selection, and validation.
public class ProjectService {

    private final ProjectDao projectDao;
    private Project currentActiveProject;

    public ProjectService(ProjectDao projectDao) {
        this.projectDao = projectDao;
        initDefaultProject();
    }

    private void initDefaultProject() {
        try {
            List<Project> projects = projectDao.findAll();
            if (!projects.isEmpty()) {
                this.currentActiveProject = projects.get(0);
            } else {
                Project def = new Project("proj-default-01", "Core Engineering & Research", "Default intellectual workspace", "ACTIVE", null);
                projectDao.create(def);
                this.currentActiveProject = def;
            }
        } catch (SQLException e) {
            // Memory fallback
            this.currentActiveProject = new Project("proj-default-01", "Core Engineering & Research", "Default intellectual workspace");
        }
    }

    public Project createProject(String name, String description) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }
        Project project = new Project(name.trim(), description != null ? description.trim() : "");
        projectDao.create(project);
        this.currentActiveProject = project;
        return project;
    }

    public List<Project> listAllProjects() throws SQLException {
        return projectDao.findAll();
    }

    public Project getProjectById(String projectId) throws SQLException {
        return projectDao.findById(projectId);
    }

    public boolean setActiveProject(String projectId) throws SQLException {
        Project p = projectDao.findById(projectId);
        if (p != null) {
            this.currentActiveProject = p;
            return true;
        }
        return false;
    }

    public Project getActiveProject() {
        return currentActiveProject;
    }

    public boolean deleteProject(String projectId) throws SQLException {
        return projectDao.delete(projectId);
    }
}
