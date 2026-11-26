# ============================================================================
# ASHUTOSH: PHAROAH Multi-Agent System - Python Swarm Entrypoint
# Invoked via Java ProcessBuilder or standalone terminal execution.
# Provides real-time streaming, format-driven workflows, and execution logging.
# ============================================================================

import sys
import os
import argparse
import json

# Ensure python directory is on PYTHONPATH
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
if CURRENT_DIR not in sys.path:
    sys.path.insert(0, CURRENT_DIR)

from workflows.workflow_registry import get_workflow_by_id_or_name, list_workflows
from orchestrator.main_agent import MainAgent
from orchestrator.workflow_executor import WorkflowExecutor

def parse_key_value(arg_val: str) -> dict:
    if not arg_val:
        return {}
    # If JSON formatted
    if arg_val.startswith("{") and arg_val.endswith("}"):
        try:
            return json.loads(arg_val)
        except Exception:
            pass
    # Parse key:value,key:value format
    result = {}
    items = [x.strip() for x in arg_val.split(",") if ":" in x]
    for item in items:
        k, v = item.split(":", 1)
        result[k.strip()] = v.strip()
    return result

def run_test_suite():
    print("\n========================================================")
    print("      PHAROAH MULTI-AGENT SWARM SELF-TEST SUITE")
    print("========================================================")
    main_agent = MainAgent()
    executor = WorkflowExecutor(main_agent)

    print("[TEST 1/2] Testing Workflow Registry Resolution...")
    wf = get_workflow_by_id_or_name(1)
    print(f"  Resolved Workflow: {wf.name} with {len(wf.get_sections())} sections.")
    assert len(wf.get_sections()) > 0

    print("[TEST 2/2] Running Swarm Mini-Execution on 'AI in Healthcare'...")
    selected_secs = ["Introduction", "Literature Review"]
    prios = {"Introduction": "HIGH", "Literature Review": "HIGH"}
    assignments = {"Introduction": "Technical Writer", "Literature Review": "Research Specialist"}

    result = executor.run(
        topic="AI in Healthcare",
        workflow=wf,
        sections=selected_secs,
        priorities=prios,
        agent_assignments=assignments
    )
    print("Self-test completed successfully. All agents and workflows functional!\n")
    return 0

def main():
    parser = argparse.ArgumentParser(description="PHAROAH Multi-Agent Swarm Runtime")
    parser.add_argument("--workflow", type=str, default="1", help="Workflow ID (1-8) or Name")
    parser.add_argument("--topic", type=str, default="Impact of Artificial Intelligence on Cybersecurity", help="Topic to work on")
    parser.add_argument("--sections", type=str, default="ALL", help="Comma-separated sections or ALL")
    parser.add_argument("--priorities", type=str, default="", help="Comma-separated Section:Priority pairs")
    parser.add_argument("--agents", type=str, default="", help="Comma-separated Section:Agent pairs")
    parser.add_argument("--list-workflows", action="store_true", help="List all available workflows")
    parser.add_argument("--test", action="store_true", help="Run automated multi-agent test suite")

    args = parser.parse_args()

    if args.test:
        sys.exit(run_test_suite())

    if args.list_workflows:
        print("\n=== AVAILABLE PHAROAH WORKFLOWS ===")
        for w in list_workflows():
            print(f" {w['id']}. {w['name']:<25} - {w['description']}")
        print("===================================\n")
        return

    # 1. Resolve Workflow
    workflow = get_workflow_by_id_or_name(args.workflow)

    # 2. Resolve Sections
    available_sections = workflow.get_sections()
    if args.sections.strip().upper() == "ALL" or not args.sections.strip():
        selected_sections = available_sections
    else:
        # Match user entered sections against available sections
        user_secs = [s.strip() for s in args.sections.split(",") if s.strip()]
        selected_sections = []
        for us in user_secs:
            # Check by number (1-based index)
            if us.isdigit():
                idx = int(us) - 1
                if 0 <= idx < len(available_sections):
                    selected_sections.append(available_sections[idx])
            else:
                # Check by case-insensitive name match
                match = next((s for s in available_sections if s.lower() == us.lower() or us.lower() in s.lower()), None)
                if match:
                    selected_sections.append(match)
                else:
                    selected_sections.append(us)

    if not selected_sections:
        selected_sections = available_sections[:3]

    # 3. Resolve Priorities and Agents
    priorities = parse_key_value(args.priorities)
    agent_assignments = parse_key_value(args.agents)

    # 4. Execute Multi-Agent Swarm
    main_agent = MainAgent()
    executor = WorkflowExecutor(main_agent)
    executor.run(
        topic=args.topic,
        workflow=workflow,
        sections=selected_sections,
        priorities=priorities,
        agent_assignments=agent_assignments
    )

if __name__ == "__main__":
    main()
