# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Data Analysis
# Empirical quantitative analytics, statistical tests, and insight extraction.
# ============================================================================

from workflows.base_workflow import BaseWorkflow

class DataAnalysisWorkflow(BaseWorkflow):
    def __init__(self):
        super().__init__(
            name="Data Analysis",
            description="Statistical data examination with quantitative matrices and predictive insights",
            sections_config=[
                {"name": "Analysis Objective", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": []},
                {"name": "Data Ingestion & Scope", "default_agent": "Data & Metrics Analyst", "default_priority": "HIGH", "dependencies": ["Analysis Objective"]},
                {"name": "Statistical Metrics", "default_agent": "Data & Metrics Analyst", "default_priority": "HIGH", "dependencies": ["Data Ingestion & Scope"]},
                {"name": "Pattern Detection", "default_agent": "Analysis Specialist", "default_priority": "MEDIUM", "dependencies": ["Statistical Metrics"]},
                {"name": "Actionable Insights", "default_agent": "Research Specialist", "default_priority": "HIGH", "dependencies": ["Pattern Detection"]}
            ]
        )
