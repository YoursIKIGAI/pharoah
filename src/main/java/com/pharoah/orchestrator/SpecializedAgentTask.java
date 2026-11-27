package com.pharoah.orchestrator;

import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.llm.LlmClient;
import com.pharoah.llm.MockLlmClient;
import com.pharoah.llm.PromptLoader;

import java.util.Collections;
import java.util.concurrent.Callable;

// VARIJ: Concurrent task unit executed by worker threads in the ExecutorService.
// Builds dynamic prompts from (role system prompt + task + dependsOn upstream outputs)
// and invokes LlmClient (or Python smolagents tool agent when --tools is enabled).
public class SpecializedAgentTask implements Callable<AgentOutput> {

    private final AgentDeployment deployment;
    private final String topic;
    private final WorkflowContext context;
    private final LlmClient llmClient;
    private final PythonAgentBridge toolBridge;
    private final boolean useTools;
    private final long simulatedWorkDurationMs;

    public SpecializedAgentTask(AgentDeployment deployment,
                                String topic,
                                WorkflowContext context,
                                LlmClient llmClient,
                                PythonAgentBridge toolBridge,
                                boolean useTools) {
        this.deployment = deployment;
        this.topic = topic;
        this.context = context;
        this.llmClient = llmClient != null ? llmClient : new MockLlmClient();
        this.toolBridge = toolBridge;
        this.useTools = useTools;
        this.simulatedWorkDurationMs = 0;
    }

    public SpecializedAgentTask(AgentDeployment deployment, String topic, WorkflowContext context, long simulatedWorkDurationMs) {
        this.deployment = deployment;
        this.topic = topic;
        this.context = context;
        this.llmClient = new MockLlmClient();
        this.toolBridge = null;
        this.useTools = false;
        this.simulatedWorkDurationMs = Math.max(0, simulatedWorkDurationMs);
    }

    @Override
    public AgentOutput call() {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        deployment.setAssignedThreadName(threadName);

        try {
            // 1. Retrieve upstream findings from completed dependencies
            String upstreamData = context.getUpstreamSynthesis(deployment.getDependencies());

            // 2. Load system prompt for this agent's specific role from resources/prompts/<role>.txt
            String roleName = (deployment.getRole() != null && !deployment.getRole().isBlank()) 
                    ? deployment.getRole().toLowerCase() : "researcher";
            String systemPrompt = PromptLoader.loadPrompt(roleName);

            // 3. Construct user prompt combining topic, task, and upstream outputs
            StringBuilder userPromptBuilder = new StringBuilder();
            userPromptBuilder.append("OVERALL TOPIC: ").append(topic).append("\n\n");
            userPromptBuilder.append("YOUR ASSIGNED TASK: ").append(deployment.getResponsibility()).append("\n\n");
            if (upstreamData != null && !upstreamData.isBlank() && !upstreamData.startsWith("No prior")) {
                userPromptBuilder.append("SYNTHESIZED UPSTREAM AGENT FINDINGS (Build directly upon these):\n")
                                 .append(upstreamData).append("\n\n");
            }
            userPromptBuilder.append("Generate a comprehensive, rigorous, and logically structured analysis for your section.");

            if (simulatedWorkDurationMs > 0) {
                Thread.sleep(simulatedWorkDurationMs);
            }

            String content;
            // 4. Step 4: Tool-using agent in Python if enabled and role is researcher
            if (useTools && toolBridge != null && "researcher".equalsIgnoreCase(roleName)) {
                System.out.printf("  [TOOL] Agent %s is utilizing Python smolagents with web search...\n", deployment.getName());
                content = toolBridge.runToolAgent(topic, deployment.getResponsibility(), llmClient.getModelName(), "http://localhost:11434", llmClient.isMock());
            } else {
                // Real LLM call
                content = llmClient.chat(systemPrompt, userPromptBuilder.toString());
            }

            if (llmClient.isMock() && !content.startsWith("[MOCK]")) {
                content = "[MOCK] " + content;
            }

            long duration = System.currentTimeMillis() - startTime;
            AgentOutput output = new AgentOutput(
                    deployment.getId(),
                    deployment.getName(),
                    deployment.getResponsibility(),
                    content,
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
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            AgentOutput failure = AgentOutput.failure(deployment.getId(), deployment.getName(), deployment.getResponsibility(),
                    "Execution error: " + t.getMessage(), duration);
            context.recordOutput(deployment.getId(), failure);
            return failure;
        }
    }
}
