# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Research Analysis
# Deep analytical inquiry, hypothesis evaluation, and evidence synthesis.
# ============================================================================

from workflows.base_workflow import BaseWorkflow

class ResearchAnalysisWorkflow(BaseWorkflow):
    def __init__(self):
        super().__init__(
            name="Research Analysis",
            description="Deep analytical inquiry, empirical evaluation, and hypothesis testing",
            sections_config=[
                {"name": "Research Question", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": []},
                {"name": "Literature Context", "default_agent": "Research Specialist", "default_priority": "HIGH", "dependencies": ["Research Question"]},
                {"name": "Data Collection", "default_agent": "Data & Metrics Analyst", "default_priority": "MEDIUM", "dependencies": []},
                {"name": "Evidence Analysis", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": ["Data Collection"]},
                {"name": "Findings & Implications", "default_agent": "Research Specialist", "default_priority": "HIGH", "dependencies": ["Evidence Analysis"]},
                {"name": "Conclusion", "default_agent": "Technical Writer", "default_priority": "MEDIUM", "dependencies": ["Findings & Implications"]}
            ]
        )
