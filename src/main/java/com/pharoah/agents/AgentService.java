package com.pharoah.agents;

import com.pharoah.persistence.dao.AgentDao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// VARIJ: Implemented AgentService.
// Manages registration, configuration, role assignment, and lifecycle of configurable swarm agents.
public class AgentService {

    private final AgentDao agentDao;

    public AgentService(AgentDao agentDao) {
        this.agentDao = agentDao;
    }

    public AgentConfig registerAgent(String name, AgentRole role, String instructions, String providerType) throws SQLException {
        AgentConfig config = new AgentConfig(name, role, instructions, providerType);
        agentDao.create(config);
        return config;
    }

    public List<AgentConfig> listAllAgents() throws SQLException {
        return agentDao.findAll();
    }

    public AgentConfig getAgentById(String agentId) throws SQLException {
        return agentDao.findById(agentId);
    }

    public Optional<AgentConfig> findBestAgentForRole(AgentRole role) throws SQLException {
        List<AgentConfig> candidates = agentDao.findByRole(role);
        if (!candidates.isEmpty()) {
            return Optional.of(candidates.get(0));
        }
        return Optional.empty();
    }

    public void setAgentActive(String agentId, boolean active) throws SQLException {
        AgentConfig agent = agentDao.findById(agentId);
        if (agent != null) {
            agent.setActive(active);
            agentDao.update(agent);
        }
    }

    public void updateInstructions(String agentId, String newInstructions) throws SQLException {
        AgentConfig agent = agentDao.findById(agentId);
        if (agent != null) {
            agent.setInstructions(newInstructions);
            agentDao.update(agent);
        }
    }
}
