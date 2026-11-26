# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Research Paper
# Canonical academic structure conforming to IEEE/ACM formatting guidelines.
# ============================================================================

from workflows.base_workflow import BaseWorkflow

class ResearchPaperWorkflow(BaseWorkflow):
    def __init__(self):
        super().__init__(
            name="Research Paper",
            description="Academic research paper adhering to IEEE/ACM peer-review standards",
            sections_config=[
                {"name": "Abstract", "default_agent": "Technical Writer", "default_priority": "MEDIUM", "dependencies": []},
                {"name": "Introduction", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": []},
                {"name": "Literature Review", "default_agent": "Research Specialist", "default_priority": "HIGH", "dependencies": []},
                {"name": "Research Gap", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": ["Literature Review"]},
                {"name": "Methodology", "default_agent": "Research Specialist", "default_priority": "HIGH", "dependencies": []},
                {"name": "Data Analysis", "default_agent": "Data & Metrics Analyst", "default_priority": "MEDIUM", "dependencies": ["Methodology"]},
                {"name": "Results and Discussion", "default_agent": "Analysis Specialist", "default_priority": "HIGH", "dependencies": ["Data Analysis"]},
                {"name": "Conclusion", "default_agent": "Technical Writer", "default_priority": "MEDIUM", "dependencies": ["Results and Discussion"]},
                {"name": "References", "default_agent": "Research Specialist", "default_priority": "LOW", "dependencies": ["Literature Review"]}
            ]
        )
