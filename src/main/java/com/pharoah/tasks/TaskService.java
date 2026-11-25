package com.pharoah.tasks;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.persistence.dao.TaskDao;

import java.sql.SQLException;
import java.util.List;

// SANCHIT: Implemented TaskService.
// Manages task creation, project association, status updates, and human review feedback.
public class TaskService {

    private final TaskDao taskDao;

    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Task createTask(String projectId, String title, TaskType type, Priority priority,
                           String inputPayload, String assignedAgentId, String dependsOnTaskId) throws SQLException {
        Task task = new Task(projectId, title, type, priority, inputPayload, assignedAgentId, dependsOnTaskId);
        taskDao.insertTask(task);
        return task;
    }

    public List<Task> getTasksByProject(String projectId) throws SQLException {
        return taskDao.findByProject(projectId);
    }

    public List<Task> getAllTasks() throws SQLException {
        return taskDao.findAll();
    }

    public Task getTaskById(String taskId) throws SQLException {
        return taskDao.findById(taskId);
    }

    public void updateStatus(String taskId, TaskStatus newStatus) throws SQLException {
        Task task = taskDao.findById(taskId);
        if (task != null) {
            task.updateStatus(newStatus);
            taskDao.updateTask(task);
        }
    }

    public void recordReviewApproval(String taskId, boolean approved, String reviewNotes) throws SQLException {
        Task task = taskDao.findById(taskId);
        if (task != null) {
            task.setReviewNotes(reviewNotes);
            if (approved) {
                task.updateStatus(TaskStatus.COMPLETED);
            } else {
                task.updateStatus(TaskStatus.IN_PROGRESS); // Request revision
            }
            taskDao.updateTask(task);
        }
    }

    public boolean deleteTask(String taskId) throws SQLException {
        return taskDao.delete(taskId);
    }
}
