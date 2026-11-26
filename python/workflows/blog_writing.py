# ============================================================================
# ASHUTOSH: PHAROAH Workflow - Blog Writing
# Engaging, high-impact technical blog posts with code illustrations and takeaways.
# ============================================================================

from workflows.base_workflow import BaseWorkflow

class BlogWritingWorkflow(BaseWorkflow):
    def __init__(self):
        super().__init__(
            name="Blog Writing",
            description="High-impact technical blog post with accessible prose and practical code examples",
            sections_config=[
                {"name": "Title & Hook", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": []},
                {"name": "Introduction", "default_agent": "Technical Writer", "default_priority": "HIGH", "dependencies": ["Title & Hook"]},
                {"name": "Core Concepts", "default_agent": "Research Specialist", "default_priority": "HIGH", "dependencies": ["Introduction"]},
                {"name": "Practical Examples & Code", "default_agent": "Data & Metrics Analyst", "default_priority": "MEDIUM", "dependencies": ["Core Concepts"]},
                {"name": "Key Takeaways", "default_agent": "Technical Writer", "default_priority": "LOW", "dependencies": ["Practical Examples & Code"]}
            ]
        )
