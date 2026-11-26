# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Report Generation
# Executive & operational reports with concrete observations and recommendations.
# ============================================================================

from workflows.base_workflow import BaseWorkflow

class ReportGenerationWorkflow(BaseWorkflow):
    def __init__(self):
        super().__init__(
            name="Report Generation",
            description="Executive and operational report generation with actionable recommendations",
            sections_config=[
                {"name": "Executive Summary", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": []},
                {"name": "Problem Statement", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": []},
                {"name": "Current Observations", "default_agent": "Research Specialist", "default_priority": "MEDIUM", "dependencies": ["Problem Statement"]},
                {"name": "Technical Analysis", "default_agent": "Data & Metrics Analyst", "default_priority": "HIGH", "dependencies": ["Current Observations"]},
                {"name": "Recommendations", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": ["Technical Analysis"]},
                {"name": "Next Steps", "default_agent": "Technical Writer", "default_priority": "LOW", "dependencies": ["Recommendations"]}
            ]
        )
