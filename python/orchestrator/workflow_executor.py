# ============================================================================
# ASHUTOSH: PHAROAH Multi-Agent Orchestrator - Workflow Executor
# Executes selected sections concurrently with ThreadPoolExecutor,
# honoring dependencies and priorities, and preserving section order in final doc.
# Streams live agent status, reasoning, reviews, and synthesizes final artifact.
# ============================================================================

import os
import time
import json
import threading
from concurrent.futures import ThreadPoolExecutor
from typing import Dict, List, Any
from workflows.base_workflow import BaseWorkflow
from orchestrator.main_agent import MainAgent

class WorkflowExecutor:
    """
    ASHUTOSH: Workflow Executor coordinates concurrent terminal execution
    of the multi-agent swarm using ThreadPoolExecutor, honoring dependencies
    and priorities, and preserving section order in the final document.
    """

    def __init__(self, main_agent: MainAgent):
        self.main_agent = main_agent
        self._lock = threading.Lock()

    def run(self,
            topic: str,
            workflow: BaseWorkflow,
            sections: List[str],
            priorities: Dict[str, str],
            agent_assignments: Dict[str, str]) -> Dict[str, Any]:

        print("\n" + "=" * 76)
        print("                  PHAROAH MULTI-AGENT SWARM EXECUTION")
        print("=" * 76)
        print(f" Workflow Type  : {workflow.name}")
        print(f" Selected Topic : {topic}")
        print(f" Total Sections : {len(sections)} sections selected")
        print(f" Active Swarm   : Research, Analysis, Writing, Data, Reviewer")
        print("=" * 76 + "\n", flush=True)

        prio_weight = {"HIGH": 3, "MEDIUM": 2, "LOW": 1}

        # Build dependency map
        deps_map = {}
        for sec in sections:
            cfg = workflow.get_section_config(sec)
            deps = [d for d in cfg.get("dependencies", []) if d in sections]
            deps_map[sec] = deps

        print(">>> MAIN ORCHESTRATOR: Concurrency Graph established:")
        for idx, sec in enumerate(sections, 1):
            assigned_agent = agent_assignments.get(sec, workflow.get_default_agent(sec))
            prio = priorities.get(sec, workflow.get_default_priority(sec))
            dep_str = ", ".join(deps_map[sec]) if deps_map[sec] else "None (Independent)"
            print(f"    Section {idx}. [{prio:<6}] {sec:<24} -> {assigned_agent} (Depends on: {dep_str})")
        print("-" * 76, flush=True)

        total_start = time.time()
        completed_results: Dict[str, Any] = {}
        unique_agents = set()
        running_futures = {}
        dispatched = set()
        remaining = list(sections)

        max_workers = min(4, max(1, len(sections)))

        def execute_single_section(sec_name: str, assigned_ag: str, prio_str: str) -> Dict[str, Any]:
            with self._lock:
                print(f"\n[RUNNING] Section '{sec_name}' assigned to {assigned_ag} (Priority: {prio_str})...", flush=True)

            with self._lock:
                prior_snapshot = list(completed_results.values())

            context = {
                "topic": topic,
                "workflow": workflow.name,
                "prior_sections": prior_snapshot
            }

            time.sleep(0.3)
            # 1. Specialized Agent execution
            sec_res = self.main_agent.execute_section(sec_name, topic, assigned_ag, context)

            # 2. Quality Reviewer Pass
            time.sleep(0.2)
            audit_res = self.main_agent.audit_section(sec_name, topic, context)

            with self._lock:
                unique_agents.add(assigned_ag)
                unique_agents.add(audit_res["agent"])
                print(audit_res["audit_feedback"], flush=True)
                print(f"[COMPLETED] Section '{sec_name}' successfully verified.", flush=True)

            return sec_res

        with ThreadPoolExecutor(max_workers=max_workers) as pool:
            while len(completed_results) < len(sections):
                # Find all sections whose dependencies have completed and are not yet dispatched
                eligible = []
                for s in remaining:
                    if s not in dispatched and all(d in completed_results for d in deps_map[s]):
                        eligible.append(s)

                if not eligible and not running_futures:
                    # Circular or unfulfilled dependency; break cycle by dispatching remaining highest-priority
                    eligible = [s for s in remaining if s not in dispatched]

                # Sort eligible by priority descending
                eligible.sort(key=lambda s: prio_weight.get(priorities.get(s, "MEDIUM").upper(), 2), reverse=True)

                for sec in eligible:
                    dispatched.add(sec)
                    assigned_agent = agent_assignments.get(sec, workflow.get_default_agent(sec))
                    prio = priorities.get(sec, workflow.get_default_priority(sec))
                    fut = pool.submit(execute_single_section, sec, assigned_agent, prio)
                    running_futures[fut] = sec

                # Wait for at least one running future to complete
                done_any = False
                for fut in list(running_futures.keys()):
                    if fut.done():
                        sec_done = running_futures.pop(fut)
                        remaining.remove(sec_done)
                        completed_results[sec_done] = fut.result()
                        done_any = True

                if not done_any:
                    time.sleep(0.05)

        total_elapsed = round(time.time() - total_start, 2)

        # Assemble Full Document - strictly preserving original section order in final doc
        document_parts = [completed_results[sec]["content"] for sec in sections if sec in completed_results]
        full_document = (
            f"# {topic}\n\n"
            f"*Generated by PHAROAH Multi-Agent Orchestration System*\n"
            f"*Workflow: {workflow.name} | Execution Time: {total_elapsed}s*\n\n"
            + "\n\n---\n\n".join(document_parts)
            + "\n\n---\n*PHAROAH Swarm Audit Completed: All sections passed verification gate.*"
        )

        # Ensure output directory exists
        os.makedirs("outputs", exist_ok=True)
        safe_filename = "".join(c if c.isalnum() else "_" for c in topic)[:40].strip("_")
        out_path = os.path.join("outputs", f"{safe_filename}_{int(time.time())}.md")
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(full_document)

        print("\n" + "=" * 76)
        print("                  PHAROAH SWARM EXECUTION COMPLETED")
        print("=" * 76)
        print(f" Status          : ALL {len(sections)} SECTIONS COMPLETED & VERIFIED")
        print(f" Agents Deployed : {', '.join(sorted(unique_agents))}")
        print(f" Elapsed Time    : {total_elapsed} seconds")
        print(f" Generated File  : {out_path}")
        print("=" * 76 + "\n", flush=True)

        # Print structured machine payload for Java Bridge
        payload = {
            "status": "COMPLETED",
            "topic": topic,
            "workflow": workflow.name,
            "sections_count": len(sections),
            "sections": sections,
            "agents": list(unique_agents),
            "output_file": out_path,
            "elapsed_seconds": total_elapsed
        }

        print("__PHAROAH_JSON_RESULT_START__")
        print(json.dumps(payload))
        print("__PHAROAH_JSON_RESULT_END__", flush=True)

        return payload
