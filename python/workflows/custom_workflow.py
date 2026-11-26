# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Custom Workflow
# Dynamic user-defined workflow allowing arbitrary section composition.
# ============================================================================

from typing import List
from workflows.base_workflow import BaseWorkflow

class CustomWorkflow(BaseWorkflow):
    def __init__(self, custom_sections: List[str] = None):
        if not custom_sections:
            custom_sections = ["Project Overview", "Core Requirements", "Implementation Plan", "Verification"]

        sections_config = []
        for sec in custom_sections:
            sec_name = sec.strip()
            # Heuristic assignment of agents based on section keywords
            lower = sec_name.lower()
            if any(k in lower for k in ["research", "literature", "background", "study", "reference"]):
                agent = "Research Specialist"
            elif any(k in lower for k in ["analysis", "gap", "comparison", "problem", "pattern"]):
                agent = "Analysis Specialist"
            elif any(k in lower for k in ["data", "metric", "statistic", "budget", "cost", "benchmark"]):
                agent = "Data & Metrics Analyst"
            elif any(k in lower for k in ["review", "audit", "test", "verification"]):
                agent = "Quality Reviewer"
            else:
                agent = "Technical Writer"

            sections_config.append({
                "name": sec_name,
                "default_agent": agent,
                "default_priority": "MEDIUM",
                "dependencies": []
            })

        super().__init__(
            name="Custom Workflow",
            description="User-defined modular workflow with automatic agent recommendation",
            sections_config=sections_config
        )
