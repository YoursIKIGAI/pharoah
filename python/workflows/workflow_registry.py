# ============================================================================
# ASHUTOSH: PHAROAH Workflow Registry
# Central registry facilitating lookup, instantiation, and cataloging of all 8 workflows.
# ============================================================================

from typing import Dict, List, Optional, Any
from workflows.base_workflow import BaseWorkflow
from workflows.research_paper import ResearchPaperWorkflow
from workflows.research_analysis import ResearchAnalysisWorkflow
from workflows.report_generation import ReportGenerationWorkflow
from workflows.blog_writing import BlogWritingWorkflow
from workflows.technical_docs import TechnicalDocumentationWorkflow
from workflows.business_proposal import BusinessProposalWorkflow
from workflows.data_analysis import DataAnalysisWorkflow
from workflows.custom_workflow import CustomWorkflow

WORKFLOW_MAP = {
    1: ("Research Paper", ResearchPaperWorkflow),
    2: ("Research Analysis", ResearchAnalysisWorkflow),
    3: ("Report Generation", ReportGenerationWorkflow),
    4: ("Blog Writing", BlogWritingWorkflow),
    5: ("Technical Documentation", TechnicalDocumentationWorkflow),
    6: ("Business Proposal", BusinessProposalWorkflow),
    7: ("Data Analysis", DataAnalysisWorkflow),
    8: ("Custom Workflow", CustomWorkflow)
}

def list_workflows() -> List[Dict[str, str]]:
    results = []
    for num, (name, cls) in WORKFLOW_MAP.items():
        if cls == CustomWorkflow:
            desc = "User-defined custom sections with dynamic agent allocation"
        else:
            instance = cls()
            desc = instance.description
        results.append({
            "id": num,
            "name": name,
            "description": desc
        })
    return results

def get_workflow_by_id_or_name(identifier: Any, custom_sections: Optional[List[str]] = None) -> BaseWorkflow:
    ident_str = str(identifier).strip().lower()

    # Match by numeric ID
    if ident_str.isdigit():
        num = int(ident_str)
        if num in WORKFLOW_MAP:
            cls = WORKFLOW_MAP[num][1]
            if cls == CustomWorkflow:
                return CustomWorkflow(custom_sections)
            return cls()

    # Match by name keywords
    for num, (name, cls) in WORKFLOW_MAP.items():
        if name.lower() in ident_str or ident_str in name.lower():
            if cls == CustomWorkflow:
                return CustomWorkflow(custom_sections)
            return cls()

    # Fallback to Research Paper
    return ResearchPaperWorkflow()
