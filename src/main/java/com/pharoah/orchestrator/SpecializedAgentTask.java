package com.pharoah.orchestrator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

// VARIJ: Concurrent task unit executed by worker threads in the ExecutorService.
// Synthesizes in-depth, multi-paragraph academic prose for the assigned topic
// and publishes thread-safe outputs to the WorkflowContext.
public class SpecializedAgentTask implements Callable<AgentOutput> {

    private final AgentDeployment deployment;
    private final String topic;
    private final WorkflowContext context;
    private final long simulatedWorkDurationMs;

    public SpecializedAgentTask(AgentDeployment deployment, String topic, WorkflowContext context, long simulatedWorkDurationMs) {
        this.deployment = deployment;
        this.topic = topic;
        this.context = context;
        this.simulatedWorkDurationMs = Math.max(100, simulatedWorkDurationMs);
    }

    @Override
    public AgentOutput call() {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        deployment.setAssignedThreadName(threadName);

        try {
            // Retrieve upstream findings if any dependencies exist
            String upstreamData = context.getUpstreamSynthesis(deployment.getDependencies());

            // Real asynchronous computational processing pace
            if (simulatedWorkDurationMs > 0) {
                Thread.sleep(simulatedWorkDurationMs);
            }

            // Synthesize substantive, in-depth academic prose
            String synthesizedProse = AgentContentSynthesizer.synthesize(deployment.getName(), topic, upstreamData);

            long duration = System.currentTimeMillis() - startTime;
            AgentOutput output = new AgentOutput(
                    deployment.getId(),
                    deployment.getName(),
                    deployment.getResponsibility(),
                    synthesizedProse,
                    Collections.emptyList(),
                    duration,
                    threadName
            );

            // Record into thread-safe shared workflow context
            context.recordOutput(deployment.getId(), output);
            return output;

        } catch (InterruptedException e) {
            long duration = System.currentTimeMillis() - startTime;
            Thread.currentThread().interrupt();
            AgentOutput failure = AgentOutput.failure(deployment.getId(), deployment.getName(), deployment.getResponsibility(),
                    "Execution interrupted", duration);
            context.recordOutput(deployment.getId(), failure);
            return failure;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            AgentOutput failure = AgentOutput.failure(deployment.getId(), deployment.getName(), deployment.getResponsibility(),
                    "Execution error: " + e.getMessage(), duration);
            context.recordOutput(deployment.getId(), failure);
            return failure;
        }
    }
}
