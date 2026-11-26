# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Technical Documentation
# Comprehensive technical documentation for systems, libraries, and frameworks.
# ============================================================================

from workflows.base_workflow import BaseWorkflow

class TechnicalDocumentationWorkflow(BaseWorkflow):
    def __init__(self):
        super().__init__(
            name="Technical Documentation",
            description="Engineering documentation covering system design, setup, architecture, and API endpoints",
            sections_config=[
                {"name": "System Overview", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": []},
                {"name": "Architecture & Modules", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": ["System Overview"]},
                {"name": "Installation & Setup", "default_agent": "Technical Writer", "default_priority": "MEDIUM", "dependencies": []},
                {"name": "Configuration", "default_agent": "Data & Metrics Analyst", "default_priority": "MEDIUM", "dependencies": ["Installation & Setup"]},
                {"name": "API Reference", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": ["Architecture & Modules"]},
                {"name": "Troubleshooting & FAQ", "default_agent": "Research Specialist", "default_priority": "LOW", "dependencies": ["Installation & Setup"]}
            ]
        )
