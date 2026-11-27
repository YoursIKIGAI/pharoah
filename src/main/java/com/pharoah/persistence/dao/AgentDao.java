package com.pharoah.persistence.dao;

import com.pharoah.agents.AgentConfig;
import com.pharoah.agents.AgentRole;
import com.pharoah.persistence.DatabaseConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// ASHUTOSH: Implemented AgentDao.
// Manages database persistence for configurable Agents (role, instructions, provider type).
public class AgentDao {

    private final DatabaseConnectionFactory connectionFactory;

    public AgentDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void create(AgentConfig agent) throws SQLException {
        String sql = "INSERT INTO agents (agent_id, name, role, instructions, provider_type, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, agent.getAgentId());
            ps.setString(2, agent.getName());
            ps.setString(3, agent.getRole().name());
            ps.setString(4, agent.getInstructions());
            ps.setString(5, agent.getProviderType());
            ps.setBoolean(6, agent.isActive());
            ps.setTimestamp(7, Timestamp.valueOf(agent.getCreatedAt()));

            ps.executeUpdate();
        }
    }

    public AgentConfig findById(String agentId) throws SQLException {
        String sql = "SELECT * FROM agents WHERE agent_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAgent(rs);
                }
            }
        }
        return null;
    }

    public AgentConfig findByName(String name) throws SQLException {
        String sql = "SELECT * FROM agents WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAgent(rs);
                }
            }
        }
        return null;
    }

    public List<AgentConfig> findByRole(AgentRole role) throws SQLException {
        String sql = "SELECT * FROM agents WHERE role = ? AND is_active = 1";
        List<AgentConfig> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAgent(rs));
                }
            }
        }
        return list;
    }

    public List<AgentConfig> findAll() throws SQLException {
        String sql = "SELECT * FROM agents ORDER BY created_at ASC";
        List<AgentConfig> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToAgent(rs));
            }
        }
        return list;
    }

    public void update(AgentConfig agent) throws SQLException {
        String sql = "UPDATE agents SET name = ?, role = ?, instructions = ?, provider_type = ?, is_active = ? WHERE agent_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, agent.getName());
            ps.setString(2, agent.getRole().name());
            ps.setString(3, agent.getInstructions());
            ps.setString(4, agent.getProviderType());
            ps.setBoolean(5, agent.isActive());
            ps.setString(6, agent.getAgentId());

            ps.executeUpdate();
        }
    }

    public boolean delete(String agentId) throws SQLException {
        String sql = "DELETE FROM agents WHERE agent_id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, agentId);
            return ps.executeUpdate() > 0;
        }
    }

    private AgentConfig mapRowToAgent(ResultSet rs) throws SQLException {
        return new AgentConfig(
                rs.getString("agent_id"),
                rs.getString("name"),
                AgentRole.valueOf(rs.getString("role")),
                rs.getString("instructions"),
                rs.getString("provider_type"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
