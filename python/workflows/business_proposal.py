# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Business Proposal
# Professional business proposals with ROI breakdown and execution roadmaps.
# ============================================================================

from workflows.base_workflow import BaseWorkflow

class BusinessProposalWorkflow(BaseWorkflow):
    def __init__(self):
        super().__init__(
            name="Business Proposal",
            description="Strategic business proposal with problem-solution mapping, budget, and ROI forecasts",
            sections_config=[
                {"name": "Executive Summary", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": []},
                {"name": "Market Problem", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": []},
                {"name": "Proposed Solution", "default_agent": "Research Specialist", "default_priority": "HIGH", "dependencies": ["Market Problem"]},
                {"name": "Implementation Roadmap", "default_agent": "Analysis Specialist", "default_priority": "MEDIUM", "dependencies": ["Proposed Solution"]},
                {"name": "Budget & ROI Forecast", "default_agent": "Data & Metrics Analyst", "default_priority": "HIGH", "dependencies": ["Implementation Roadmap"]},
                {"name": "Conclusion & Call to Action", "default_agent": "Technical Writer", "default_priority": "LOW", "dependencies": ["Budget & ROI Forecast"]}
            ]
        )
