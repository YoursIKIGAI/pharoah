# ============================================================================
# ASHUTOSH: PHAROAH Multi-Agent System - Base Agent Architecture
# Defines the core abstraction for all specialized agents in the swarm.
# Supports real-time status transitions (WAITING -> RUNNING -> COMPLETED).
# ============================================================================

import time
import sys

class BaseAgent:
    """
    ASHUTOSH: Base class for all PHAROAH autonomous swarm agents.
    Provides standardized logging, state management, and execution hooks.
    """

    def __init__(self, name: str, role: str):
        self.name = name
        self.role = role
        self.status = "IDLE"
        self.total_tasks_executed = 0

    def set_status(self, new_status: str, detail: str = ""):
        self.status = new_status
        timestamp = time.strftime("%H:%M:%S")
        status_badges = {
            "IDLE": "[IDLE]",
            "WAITING": "[WAITING]",
            "RUNNING": "[RUNNING]",
            "COMPLETED": "[COMPLETED]",
            "FAILED": "[FAILED]"
        }
        badge = status_badges.get(new_status, f"[{new_status}]")
        msg = f"  {badge} [{timestamp}] Agent: {self.name} ({self.role})"
        if detail:
            msg += f" -> {detail}"
        print(msg, flush=True)

    def log(self, message: str, indent: int = 4):
        prefix = " " * indent + ">>> "
        print(f"{prefix}{message}", flush=True)

    def execute(self, section_name: str, topic: str, context: dict) -> dict:
        """
        Subclasses must implement this method to carry out task-specific intelligence.
        """
        raise NotImplementedError("Subclasses must implement execute()")

    def __repr__(self):
        return f"<Agent name='{self.name}' role='{self.role}' status='{self.status}'>"
