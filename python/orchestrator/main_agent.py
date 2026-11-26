# ============================================================================
# ASHUTOSH: PHAROAH Multi-Agent Orchestrator - Main Agent
# Coordinates specialized sub-agents, manages hierarchical task delegation,
# resolves inter-section dependencies, and drives document synthesis.
# ============================================================================

import time
from typing import Dict, List, Any
from agents.research_agent import ResearchAgent
from agents.analysis_agent import AnalysisAgent
from agents.writing_agent import WritingAgent
from agents.data_analysis_agent import DataAnalysisAgent
from agents.reviewer_agent import ReviewerAgent

class MainAgent:
    """
    ASHUTOSH: The Main Orchestrator Agent.
    Coordinates sub-agents, dispatches tasks, and compiles the final output.
    """

    def __init__(self, name: str = "PHAROAH Orchestrator"):
        self.name = name
        self.role = "CHIEF_ORCHESTRATOR"
        # Instantiate the pool of specialized sub-agents
        self.agent_pool: Dict[str, Any] = {
            "Research Specialist": ResearchAgent(),
            "Analysis Specialist": AnalysisAgent(),
            "Technical Writer": WritingAgent(),
            "Data & Metrics Analyst": DataAnalysisAgent(),
            "Quality Reviewer": ReviewerAgent()
        }

    def get_agent(self, agent_name_or_role: str):
        # Look up by exact name
        if agent_name_or_role in self.agent_pool:
            return self.agent_pool[agent_name_or_role]

        # Look up by role substring
        lower = agent_name_or_role.lower()
        if "research" in lower:
            return self.agent_pool["Research Specialist"]
        elif "analysis" in lower or "gap" in lower:
            return self.agent_pool["Analysis Specialist"]
        elif "data" in lower or "metric" in lower or "stat" in lower:
            return self.agent_pool["Data & Metrics Analyst"]
        elif "review" in lower or "audit" in lower:
            return self.agent_pool["Quality Reviewer"]
        else:
            return self.agent_pool["Technical Writer"]

    def execute_section(self, section_name: str, topic: str, agent_name: str, context: dict) -> dict:
        agent = self.get_agent(agent_name)
        result = agent.execute(section_name, topic, context)
        return result

    def audit_section(self, section_name: str, topic: str, context: dict) -> dict:
        reviewer: ReviewerAgent = self.agent_pool["Quality Reviewer"]
        return reviewer.execute(section_name, topic, context)
