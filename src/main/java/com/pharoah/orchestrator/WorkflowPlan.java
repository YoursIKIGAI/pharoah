package com.pharoah.orchestrator;

import java.util.*;

// VARIJ: User-customizable execution plan.
// Validates DAG dependency graphs, enforces cycle detection, and bounds worker concurrency.
public class WorkflowPlan {

    private final WorkflowCategory category;
    private final String topic;
    private final List<AgentDeployment> agents;
    private int maxConcurrentWorkers;

    public WorkflowPlan(WorkflowCategory category, String topic, List<AgentDeployment> agents, int maxConcurrentWorkers) {
        this.category = category;
        this.topic = topic;
        this.agents = new ArrayList<>(agents);
        this.maxConcurrentWorkers = Math.max(1, maxConcurrentWorkers);
        validatePlan();
    }

    public WorkflowPlan(WorkflowCategory category, String topic, List<AgentDeployment> agents) {
        this(category, topic, agents, 4);
    }

    // Validates that all dependency references exist and detects cycle loops in the DAG
    public void validatePlan() {
        Set<String> allIds = new HashSet<>();
        for (AgentDeployment a : agents) {
            allIds.add(a.getId());
        }

        // Verify that dependency IDs reference actual agents
        for (AgentDeployment a : agents) {
            for (String dep : a.getDependencies()) {
                if (!allIds.contains(dep)) {
                    throw new IllegalArgumentException(String.format("Agent '%s' references unknown dependency '%s'.", a.getId(), dep));
                }
                if (dep.equals(a.getId())) {
                    throw new IllegalArgumentException(String.format("Agent '%s' cannot depend on itself.", a.getId()));
                }
            }
        }

        // Cycle detection using Kahn's algorithm
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (AgentDeployment a : agents) {
            inDegree.put(a.getId(), a.getDependencies().size());
            adj.put(a.getId(), new ArrayList<>());
        }
        for (AgentDeployment a : agents) {
            for (String dep : a.getDependencies()) {
                adj.get(dep).add(a.getId());
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        int visitedCount = 0;
        while (!queue.isEmpty()) {
            String u = queue.poll();
            visitedCount++;
            for (String v : adj.get(u)) {
                inDegree.put(v, inDegree.get(v) - 1);
                if (inDegree.get(v) == 0) {
                    queue.offer(v);
                }
            }
        }

        if (visitedCount != agents.size()) {
            throw new IllegalStateException("Cyclic dependency detected in agent deployment plan! Please resolve circular dependencies.");
        }
    }

    public WorkflowCategory getCategory() {
        return category;
    }

    public String getTopic() {
        return topic;
    }

    public List<AgentDeployment> getAgents() {
        return Collections.unmodifiableList(agents);
    }

    public int getMaxConcurrentWorkers() {
        return maxConcurrentWorkers;
    }

    public void setMaxConcurrentWorkers(int maxConcurrentWorkers) {
        this.maxConcurrentWorkers = Math.max(1, maxConcurrentWorkers);
    }

    public AgentDeployment getAgentById(String id) {
        for (AgentDeployment a : agents) {
            if (a.getId().equalsIgnoreCase(id)) return a;
        }
        return null;
    }

    public int getIndependentCount() {
        int count = 0;
        for (AgentDeployment a : agents) {
            if (a.isIndependent()) count++;
        }
        return count;
    }

    public int getDependentCount() {
        return agents.size() - getIndependentCount();
    }
}
