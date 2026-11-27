package com.pharoah.test;

import com.pharoah.core.enums.Priority;
import com.pharoah.orchestrator.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Requirement 11(a): Failing upstream agent -> workflow finishes, dependents SKIPPED.
 */
public class WorkflowFailurePropagationTest {

    @Test
    @DisplayName("Direct Context: Upstream failure transitively marks all dependent agents SKIPPED")
    public void testTransitiveFailurePropagationInContext() {
        AgentDeployment upstream = new AgentDeployment("agent-01", "Upstream Research", AgentKind.RESEARCH_PAPER, "Foundations", Priority.HIGH);
        AgentDeployment dependent = new AgentDeployment("agent-02", "Dependent Writer", AgentKind.TECHNICAL_ARCHITECTURE, "Architecture", Priority.HIGH, List.of("agent-01"));
        AgentDeployment transitiveDep = new AgentDeployment("agent-03", "Transitive Reviewer", AgentKind.STRATEGIC_ANALYSIS, "Review", Priority.MEDIUM, List.of("agent-02"));

        List<AgentDeployment> agents = List.of(upstream, dependent, transitiveDep);
        WorkflowContext context = new WorkflowContext("Research", "Failure Propagation Test", agents);

        // Before failure
        assertEquals(AgentStatus.READY, upstream.getStatus());
        assertEquals(AgentStatus.WAITING, dependent.getStatus());
        assertEquals(AgentStatus.WAITING, transitiveDep.getStatus());
        assertFalse(context.isAllCompleted());

        // Simulate upstream failure
        AgentOutput failureOutput = AgentOutput.failure("agent-01", "Upstream Research", "Foundations", "Hardware failure", 100);
        context.recordOutput("agent-01", failureOutput);

        // Verify propagation
        assertEquals(AgentStatus.FAILED, upstream.getStatus());
        assertEquals(AgentStatus.SKIPPED, dependent.getStatus());
        assertEquals(AgentStatus.SKIPPED, transitiveDep.getStatus());

        // Verify workflow completion without hang
        assertTrue(context.isAllCompleted(), "Context should report all completed after failure propagation");
        assertEquals(0, context.getCompletedTasksCount());
        assertEquals(3, context.getFailedTasksCount(), "Failed upstream + 2 skipped dependents should equal 3 failed tasks");

        // Verify outputs recorded for skipped agents
        assertNotNull(context.getOutputs().get("agent-02"));
        assertFalse(context.getOutputs().get("agent-02").isSuccess());
        assertTrue(context.getOutputs().get("agent-02").getContent().contains("Skipped due to upstream dependency"));

        assertNotNull(context.getOutputs().get("agent-03"));
        assertFalse(context.getOutputs().get("agent-03").isSuccess());
        assertTrue(context.getOutputs().get("agent-03").getContent().contains("Skipped due to upstream dependency"));
    }

    @Test
    @DisplayName("Orchestrator Swarm: Upstream failure terminates workflow cleanly without hanging")
    public void testOrchestratorFinishesOnUpstreamFailure() {
        AgentDeployment upstream = new AgentDeployment("agent-01", "Upstream", AgentKind.RESEARCH_PAPER, "Upstream Task", Priority.HIGH);
        AgentDeployment dependent = new AgentDeployment("agent-02", "Downstream", AgentKind.TECHNICAL_ARCHITECTURE, "Downstream Task", Priority.HIGH, List.of("agent-01"));

        // Set upstream to fail immediately by throwing an error or pre-recording failure in context
        WorkflowPlan plan = new WorkflowPlan(
                WorkflowCategory.CUSTOM_WORKFLOW,
                "Failure Orchestration Test",
                List.of(upstream, dependent),
                2,
                ExecutionMode.PARALLEL
        );

        // Run orchestrator with low latency and verify it finishes within 5 seconds
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator(null, 100L, 5000L);
        long start = System.currentTimeMillis();
        WorkflowExecutionResult result = orchestrator.executeWorkflow(plan);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 4000, "Workflow should finish promptly without hanging (elapsed: " + elapsed + "ms)");
        assertNotNull(result);
    }
}
