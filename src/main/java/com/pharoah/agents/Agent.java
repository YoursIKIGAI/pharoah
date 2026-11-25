package com.pharoah.agents;

import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.knowledge.SubjectMemory;

/**
 * Core polymorphic contract for all specialized agents in PHAROAH.
 * 
 * Demonstrates:
 * 1. Interface-based polymorphism: The Orchestrator delegates tasks to Agent references
 *    without coupling to specific implementations.
 * 2. Clear separation of responsibilities: Input, Execution, Output, Status, Error Handling.
 */
public interface Agent {

    String getAgentId();

    String getName();

    String getDescription();

    boolean canHandle(TaskType taskType);

    TaskResult execute(Task task, SubjectMemory memory);
}
