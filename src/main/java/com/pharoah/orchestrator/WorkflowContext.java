package com.pharoah.orchestrator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// VARIJ: Thread-safe shared workflow memory context.
// Coordinates inter-agent communication, resolves DAG dependencies,
// tracks atomic metrics, and guarantees synchronized state updates across worker threads.
public class WorkflowContext {

    private final String workflowCategory;
    private final String topic;
    private final List<AgentDeployment> deployedAgents;
    private final Map<String, AgentDeployment> agentMap = new ConcurrentHashMap<>();
    private final Map<String, AgentOutput> outputs = new ConcurrentHashMap<>();

    // Atomic thread counters
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);

    // Concurrency coordination locks
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition stateChanged = lock.newCondition();

    private final long startTimeMs;

    public WorkflowContext(String workflowCategory, String topic, List<AgentDeployment> agents) {
        this.workflowCategory = workflowCategory;
        this.topic = topic;
        this.deployedAgents = new ArrayList<>(agents);
        for (AgentDeployment a : agents) {
            agentMap.put(a.getId(), a);
        }
        this.startTimeMs = System.currentTimeMillis();
    }

    // VARIJ: Inter-thread communication: dependent agents read upstream completed results
    public String getUpstreamSynthesis(List<String> prerequisiteIds) {
        if (prerequisiteIds == null || prerequisiteIds.isEmpty()) {
            return "No prior dependencies required for this task.";
        }

        StringBuilder sb = new StringBuilder();
        for (String pid : prerequisiteIds) {
            AgentOutput out = outputs.get(pid);
            if (out != null) {
                sb.append("--- Upstream Findings from ").append(out.getAgentName())
                  .append(" (").append(out.getResponsibility()).append(") ---\n")
                  .append(out.getContent()).append("\n\n");
            }
        }
        return sb.toString();
    }

    // Records completed agent output and signals waiting threads
    public void recordOutput(String agentId, AgentOutput output) {
        lock.lock();
        try {
            outputs.put(agentId, output);
            AgentDeployment deployment = agentMap.get(agentId);
            if (deployment != null) {
                if (output.isSuccess()) {
                    deployment.setStatus(AgentStatus.COMPLETED);
                    completedTasks.incrementAndGet();
                } else {
                    deployment.setStatus(AgentStatus.FAILED);
                    failedTasks.incrementAndGet();
                }
                deployment.setExecutionDurationMs(output.getDurationMs());
                deployment.setAssignedThreadName(output.getThreadName());
                String snippet = output.getContent().lines().findFirst().orElse("");
                if (snippet.length() > 50) snippet = snippet.substring(0, 47) + "...";
                deployment.setOutputSnippet(snippet);
            }
            activeThreads.decrementAndGet();

            // Wake up orchestrator to dispatch newly unblocked dependent agents
            stateChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Checks if all prerequisites for a given agent have successfully completed
    public boolean areDependenciesSatisfied(AgentDeployment agent) {
        if (agent.isIndependent()) {
            return true;
        }
        for (String depId : agent.getDependencies()) {
            AgentOutput out = outputs.get(depId);
            if (out == null || !out.isSuccess()) {
                return false;
            }
        }
        return true;
    }

    // Returns all agents currently eligible to execute, sorted by Priority (HIGH -> MEDIUM -> LOW)
    public List<AgentDeployment> getEligibleReadyAgents() {
        lock.lock();
        try {
            List<AgentDeployment> ready = new ArrayList<>();
            for (AgentDeployment a : deployedAgents) {
                if (a.getStatus() == AgentStatus.READY || (a.getStatus() == AgentStatus.WAITING && areDependenciesSatisfied(a))) {
                    ready.add(a);
                }
            }
            // Sort by priority weight: HIGH (1), MEDIUM (2), LOW (3)
            ready.sort((a, b) -> a.getPriority().compareTo(b.getPriority()));
            return ready;
        } finally {
            lock.unlock();
        }
    }

    public void markAgentRunning(String agentId, String threadName) {
        lock.lock();
        try {
            AgentDeployment a = agentMap.get(agentId);
            if (a != null) {
                a.setStatus(AgentStatus.RUNNING);
                a.setAssignedThreadName(threadName);
            }
            activeThreads.incrementAndGet();
            stateChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isAllCompleted() {
        return (completedTasks.get() + failedTasks.get()) >= deployedAgents.size();
    }

    public void awaitStateChange(long timeoutMs) throws InterruptedException {
        lock.lock();
        try {
            if (!isAllCompleted()) {
                stateChanged.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    public int getOverallProgressPercent() {
        if (deployedAgents.isEmpty()) return 100;
        int finished = completedTasks.get() + failedTasks.get();
        return (int) Math.round(((double) finished / deployedAgents.size()) * 100.0);
    }

    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    // --- Getters ---

    public String getWorkflowCategory() {
        return workflowCategory;
    }

    public String getTopic() {
        return topic;
    }

    public List<AgentDeployment> getDeployedAgents() {
        return Collections.unmodifiableList(deployedAgents);
    }

    public Map<String, AgentOutput> getOutputs() {
        return Collections.unmodifiableMap(outputs);
    }

    public int getActiveThreadsCount() {
        return activeThreads.get();
    }

    public int getCompletedTasksCount() {
        return completedTasks.get();
    }

    public int getFailedTasksCount() {
        return failedTasks.get();
    }

    public int getPendingWaitingCount() {
        int count = 0;
        for (AgentDeployment a : deployedAgents) {
            if (a.getStatus() == AgentStatus.WAITING || a.getStatus() == AgentStatus.READY) {
                count++;
            }
        }
        return count;
    }
}
