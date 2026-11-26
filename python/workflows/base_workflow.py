# ============================================================================
# ASHUTOSH: PHAROAH Multi-Agent System - Base Workflow Definition
# Standardizes format templates, section decompositions, default agent assignments,
# and dependency mapping for intellectual workflows.
# ============================================================================

from typing import List, Dict, Any

class BaseWorkflow:
    """
    ASHUTOSH: Base class for all document format workflows in PHAROAH.
    Maps document sections to specialized agents and recommended execution priorities.
    """

    def __init__(self, name: str, description: str, sections_config: List[Dict[str, Any]]):
        self.name = name
        self.description = description
        self.sections_config = sections_config

    def get_sections(self) -> List[str]:
        return [sec["name"] for sec in self.sections_config]

    def get_section_config(self, section_name: str) -> Dict[str, Any]:
        for sec in self.sections_config:
            if sec["name"].lower() == section_name.lower():
                return sec
        # Default fallback
        return {
            "name": section_name,
            "default_agent": "Technical Writer",
            "default_priority": "MEDIUM",
            "dependencies": []
        }

    def get_default_agent(self, section_name: str) -> str:
        return self.get_section_config(section_name).get("default_agent", "Technical Writer")

    def get_default_priority(self, section_name: str) -> str:
        return self.get_section_config(section_name).get("default_priority", "MEDIUM")

    def get_dependencies(self, section_name: str) -> List[str]:
        return self.get_section_config(section_name).get("dependencies", [])

    def display_format(self):
        print(f"\n========================================")
        print(f" {self.name.upper()} WORKFLOW")
        print(f" {self.description}")
        print(f"========================================")
        for idx, sec in enumerate(self.sections_config, 1):
            agent = sec.get("default_agent", "Writing Agent")
            prio = sec.get("default_priority", "MEDIUM")
            deps = sec.get("dependencies", [])
            dep_str = f" (Depends on: {', '.join(deps)})" if deps else ""
            print(f"  {idx:2d}. {sec['name']:<25} | Recommended Agent: {agent:<22} | Prio: {prio}{dep_str}")
        print("========================================\n")
