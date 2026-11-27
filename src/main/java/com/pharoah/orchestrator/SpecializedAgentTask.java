package com.pharoah.orchestrator;

import com.pharoah.agents.Agent;
import com.pharoah.agents.AgentRegistry;
import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;
import com.pharoah.core.model.TaskResult;
import com.pharoah.llm.LlmClient;
import com.pharoah.llm.MockLlmClient;
import com.pharoah.llm.PromptLoader;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.Callable;

// VARIJ: Concurrent task unit executed by worker threads in the ExecutorService.
// Routes execution to polymorphic specialized open-source agents in AgentRegistry,
// leveraging Ollama LLM reasoning or Python smolagents live search tools.
public class SpecializedAgentTask implements Callable<AgentOutput> {

    private final AgentDeployment deployment;
    private final String topic;
    private final WorkflowContext context;
    private final LlmClient llmClient;
    private final PythonAgentBridge toolBridge;
    private final boolean useTools;
    private final long simulatedWorkDurationMs;
    private final AgentRegistry agentRegistry;

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
        this.agentRegistry = new AgentRegistry(this.llmClient);
    }

    public SpecializedAgentTask(AgentDeployment deployment, String topic, WorkflowContext context, long simulatedWorkDurationMs) {
        this.deployment = deployment;
        this.topic = topic;
        this.context = context;
        this.llmClient = new MockLlmClient();
        this.toolBridge = null;
        this.useTools = false;
        this.simulatedWorkDurationMs = Math.max(0, simulatedWorkDurationMs);
        this.agentRegistry = new AgentRegistry(this.llmClient);
    }

    @Override
    public AgentOutput call() {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        deployment.setAssignedThreadName(threadName);

        try {
            // 1. Retrieve upstream findings from completed dependencies
            String upstreamData = context.getUpstreamSynthesis(deployment.getDependencies());

            // 2. Identify agent specialty role
            String roleName = (deployment.getRole() != null && !deployment.getRole().isBlank()) 
                    ? deployment.getRole().toLowerCase() : "researcher";

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

            String content = null;
            // 4. Dispatch: Tool-using agent in Python if enabled and role is researcher
            boolean toolSuccess = false;
            if (useTools && toolBridge != null && "researcher".equalsIgnoreCase(roleName)) {
                try {
                    System.out.printf("  [TOOL] Agent %s is utilizing Python smolagents with web search...\n", deployment.getName());
                    content = toolBridge.runToolAgent(topic, deployment.getResponsibility(), llmClient.getModelName(), "http://localhost:11434", llmClient.isMock());
                    toolSuccess = true;
                } catch (Exception ex) {
                    System.err.printf("  [TOOL WARNING] Python tool agent failed: %s. Falling back cleanly to Java agent execution.\n", ex.getMessage());
                }
            }
            if (!toolSuccess) {
                // 5. Polymorphic execution through specialized open-source agent in AgentRegistry
                Agent agent = agentRegistry.getAgentForRole(roleName);
                if (agent != null) {
                    TaskType taskType = resolveTaskType(roleName);
                    Task task = new Task(
                            deployment.getId(),
                            "PHAROAH-RUN",
                            deployment.getName(),
                            taskType,
                            deployment.getPriority(),
                            userPromptBuilder.toString(),
                            deployment.getId(),
                            null,
                            TaskStatus.PENDING,
                            null,
                            null,
                            LocalDateTime.now(),
                            null
                    );
                    TaskResult result = agent.execute(task, null);
                    content = result.output();
                } else {
                    String systemPrompt = PromptLoader.loadPrompt(roleName);
                    content = llmClient.chat(systemPrompt, userPromptBuilder.toString());
                }
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

    private TaskType resolveTaskType(String role) {
        String r = role.toLowerCase();
        if (r.contains("analyst") || r.contains("data")) return TaskType.DATA_ANALYSIS;
        if (r.contains("writer") || r.contains("synthesis") || r.contains("author")) return TaskType.WRITING;
        if (r.contains("critic") || r.contains("verifier") || r.contains("review")) return TaskType.VERIFICATION;
        if (r.contains("plan")) return TaskType.PLANNING;
        if (r.contains("summary") || r.contains("summariser")) return TaskType.DOCUMENT_ANALYSIS;
        if (r.contains("compare")) return TaskType.COMPARISON;
        if (r.contains("study") || r.contains("tutor")) return TaskType.STUDY_ASSISTANCE;
        return TaskType.RESEARCH;
    }
}
