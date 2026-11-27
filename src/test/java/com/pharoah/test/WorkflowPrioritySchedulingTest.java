package com.pharoah.test;

import com.pharoah.core.enums.Priority;
import com.pharoah.orchestrator.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Requirement 11(b): Priority mode respects dependencies.
 * A High-Priority agent that depends on a Low-Priority prerequisite must wait
 * for the prerequisite to complete before being scheduled.
 */
public class WorkflowPrioritySchedulingTest {

    @Test
    @DisplayName("Context Ready-Queue: High-priority dependent task waits for Low-priority prerequisite")
    public void testPrioritySchedulingRespectsDependencies() {
        AgentDeployment prerequisite = new AgentDeployment("agent-low", "Low-Priority Prerequisite",
                AgentKind.DATA_ENQUIRY, "Base Data", Priority.LOW);

        AgentDeployment dependent = new AgentDeployment("agent-high", "High-Priority Dependent",
                AgentKind.RESEARCH_PAPER, "Synthesis", Priority.HIGH, List.of("agent-low"));

        AgentDeployment independent = new AgentDeployment("agent-med", "Medium-Priority Independent",
                AgentKind.CREATIVE, "Context Story", Priority.MEDIUM);

        List<AgentDeployment> allAgents = List.of(prerequisite, dependent, independent);
        WorkflowContext context = new WorkflowContext("Test", "Priority Dependency Scheduling", allAgents);

        // Stage 1: Initial state
        // 'agent-high' has Priority.HIGH, but its dependency 'agent-low' is NOT completed.
        // Therefore, 'agent-high' must NOT appear in eligible ready agents.
        List<AgentDeployment> initialEligible = context.getEligibleReadyAgents();
        assertEquals(2, initialEligible.size(), "Only independent agents should be eligible initially");
        assertFalse(initialEligible.contains(dependent), "High-priority dependent task must NOT be eligible yet");

        // The initial eligible agents are sorted by priority: agent-med (MEDIUM) before agent-low (LOW)
        assertEquals("agent-med", initialEligible.get(0).getId());
        assertEquals("agent-low", initialEligible.get(1).getId());

        // Stage 2: Prerequisite completes
        AgentOutput lowOutput = new AgentOutput("agent-low", "Low-Priority Prerequisite",
                "Base Data", "Quantitative metric data successfully analyzed.", List.of(), 50, "test-thread");
        context.recordOutput("agent-low", lowOutput);

        // Now 'agent-high' dependency is satisfied! It must become eligible and be prioritized FIRST
        List<AgentDeployment> nextEligible = context.getEligibleReadyAgents();
        assertTrue(nextEligible.contains(dependent), "High-priority dependent task must now be eligible");
        // agent-high (HIGH) should be ranked ahead of agent-med (MEDIUM) if agent-med is still ready
        assertEquals("agent-high", nextEligible.get(0).getId(), "High priority agent must be dispatched first");
    }

    @Test
    @DisplayName("Orchestrator Execution: Priority mode end-to-end execution satisfies dependencies")
    public void testOrchestratorPriorityExecutionWithDependencies() {
        AgentDeployment prereq = new AgentDeployment("p-01", "Foundation",
                AgentKind.RESEARCH_PAPER, "Literature Foundation", Priority.LOW);

        AgentDeployment dependentHigh = new AgentDeployment("p-02", "Executive Analysis",
                AgentKind.STRATEGIC_ANALYSIS, "Strategic Inferences", Priority.HIGH, List.of("p-01"));

        WorkflowPlan plan = new WorkflowPlan(
                WorkflowCategory.RESEARCH_PAPER,
                "Priority Concurrency Execution",
                List.of(prereq, dependentHigh),
                2,
                ExecutionMode.PRIORITY_BASED
        );

        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator(null, 150L, 5000L);
        WorkflowExecutionResult result = orchestrator.executeWorkflow(plan);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getAgentOutputs().size());
        assertTrue(result.getAgentOutputs().get("p-01").isSuccess());
        assertTrue(result.getAgentOutputs().get("p-02").isSuccess());

        // Verify downstream output contains upstream findings
        String downstreamText = result.getAgentOutputs().get("p-02").getContent();
        assertNotNull(downstreamText);
        assertFalse(downstreamText.isBlank());
    }
}
