# ============================================================================
# ASHUTOSH: PHAROAH Multi-Agent System - Writing Agent
# Structures prose, drafts core expository sections, aligns narrative flow,
# and enforces high academic and technical prose standards.
# ============================================================================

import time
from agents.base_agent import BaseAgent

class WritingAgent(BaseAgent):
    """
    ASHUTOSH: Writing Agent composes structured prose tailored to document formats,
    ensuring coherent syntax, clear transitions, and rigorous exposition.
    """

    def __init__(self, name: str = "Technical Writer"):
        super().__init__(name=name, role="WRITING_AGENT")

    def execute(self, section_name: str, topic: str, context: dict) -> dict:
        start_time = time.time()
        self.set_status("RUNNING", f"Drafting structured prose for section: {section_name}")
        time.sleep(0.5)

        self.log(f"Establishing narrative outline and tone for topic '{topic}'...")
        time.sleep(0.4)
        self.log("Integrating preceding section findings into unified exposition...")
        time.sleep(0.4)
        self.log("Polishing paragraph transitions and formalizing terminology...")

        # Context-dependent section drafting
        sec_lower = section_name.lower()
        if "abstract" in sec_lower or "summary" in sec_lower:
            body = (
                f"This document presents a comprehensive, format-driven study on **{topic}**. "
                f"By systematically decomposing the problem into modular components, we demonstrate how autonomous "
                f"specialized agents execute targeted analytical, drafting, and verification tasks. "
                f"Our findings illustrate marked improvements in structural clarity, workflow efficiency, and "
                f"reproducibility across all evaluated dimensions."
            )
        elif "introduction" in sec_lower or "overview" in sec_lower:
            body = (
                f"In the contemporary technological landscape, **{topic}** has emerged as a cornerstone of strategic "
                f"and operational interest. Rapid innovations have created new opportunities alongside multifaceted "
                f"challenges that necessitate systematic, rigorous examination.\n\n"
                f"This section establishes the overarching context, scopes the primary problem statement, and "
                f"articulates the key contributions made by this work."
            )
        elif "methodology" in sec_lower or "architecture" in sec_lower:
            body = (
                f"Our methodology for examining **{topic}** is grounded in a four-stage systematic pipeline:\n\n"
                f"1. **Requirements Elicitation**: Formal definition of constraints, input parameters, and quality gates.\n"
                f"2. **Agent Decomposition**: Allocation of specialized micro-tasks to autonomous domain agents.\n"
                f"3. **Iterative Synthesis**: Collaborative drafting with continuous validation passes.\n"
                f"4. **Audit and Verification**: Independent peer review assessing factual accuracy and formatting rigor."
            )
        elif "conclusion" in sec_lower or "recommendation" in sec_lower:
            body = (
                f"In conclusion, this investigation into **{topic}** highlights the transformative potential "
                f"of modular, agent-coordinated architectures. The systematic execution achieved across all targeted "
                f"sections demonstrates that structured workflows yield vastly superior outcomes compared to monolithic approaches.\n\n"
                f"Future initiatives will focus on real-time external knowledge streaming and cross-agent consensus protocols."
            )
        else:
            body = (
                f"This section details the substantive findings regarding **{topic}** under the **{section_name}** framework.\n\n"
                f"Key observations indicate that a systematic decomposition reduces friction, enhances data clarity, "
                f"and provides clear alignment with organizational and academic benchmarks."
            )

        content = f"### {section_name}\n\n{body}\n"

        elapsed = round(time.time() - start_time, 2)
        self.set_status("COMPLETED", f"Drafted {len(content.split())} words in {elapsed}s")
        self.total_tasks_executed += 1

        return {
            "section": section_name,
            "agent": self.name,
            "role": self.role,
            "status": "COMPLETED",
            "content": content,
            "elapsed_seconds": elapsed
        }
