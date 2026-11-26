package com.pharoah.orchestrator;

import com.pharoah.cli.TerminalUI;

import java.util.List;

// VARIJ: Live Terminal Dashboard.
// Dynamically renders real-time thread bindings, task statuses,
// active worker counts, and overall workflow progress in the terminal.
public class DashboardView {

    public static void render(WorkflowContext context, int poolSize) {
        StringBuilder sb = new StringBuilder();

        sb.append(TerminalUI.CYAN).append("╔═══════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                   PHAROAH MULTI-AGENT ORCHESTRATOR                    ║\n");
        sb.append("╚═══════════════════════════════════════════════════════════════════════╝").append(TerminalUI.RESET).append("\n");

        sb.append(String.format(" %sWorkflow:%s %-20s │ %sTopic:%s %s\n",
                TerminalUI.DIM, TerminalUI.RESET, context.getWorkflowCategory(),
                TerminalUI.DIM, TerminalUI.RESET, context.getTopic()));
        sb.append(String.format(" %sWorker Pool:%s %d Threads        │ %sActive Tasks:%s %d Threads Running\n",
                TerminalUI.DIM, TerminalUI.RESET, poolSize,
                TerminalUI.DIM, TerminalUI.RESET, context.getActiveThreadsCount()));
        sb.append(TerminalUI.DIM).append("─".repeat(73)).append(TerminalUI.RESET).append("\n\n");

        List<AgentDeployment> agents = context.getDeployedAgents();
        for (AgentDeployment a : agents) {
            String badge;
            String detail;

            switch (a.getStatus()) {
                case RUNNING -> {
                    badge = TerminalUI.CYAN + "[⏳ RUNNING  ]" + TerminalUI.RESET;
                    detail = TerminalUI.DIM + "[Thread: " + a.getAssignedThreadName() + "]" + TerminalUI.RESET;
                }
                case COMPLETED -> {
                    badge = TerminalUI.GREEN + "[✓ COMPLETED]" + TerminalUI.RESET;
                    detail = TerminalUI.GREEN + String.format("[%d ms on %s]", a.getExecutionDurationMs(), a.getAssignedThreadName()) + TerminalUI.RESET;
                }
                case WAITING -> {
                    badge = TerminalUI.YELLOW + "[! WAITING  ]" + TerminalUI.RESET;
                    detail = TerminalUI.YELLOW + "[Needs: " + String.join(", ", a.getDependencies()) + "]" + TerminalUI.RESET;
                }
                case READY -> {
                    badge = TerminalUI.BLUE + "[▶ READY    ]" + TerminalUI.RESET;
                    detail = TerminalUI.DIM + "[Queued for execution]" + TerminalUI.RESET;
                }
                case FAILED -> {
                    badge = TerminalUI.RED + "[✗ FAILED   ]" + TerminalUI.RESET;
                    detail = TerminalUI.RED + "[Execution error]" + TerminalUI.RESET;
                }
                default -> {
                    badge = "[? UNKNOWN  ]";
                    detail = "";
                }
            }

            sb.append(String.format("  %s %s (%-25s) %s\n",
                    badge, a.getName(), a.getPriority() + " Priority", detail));
        }

        sb.append("\n").append(TerminalUI.DIM).append("─".repeat(73)).append(TerminalUI.RESET).append("\n");

        // Progress bar calculation
        int progress = context.getOverallProgressPercent();
        int barLength = 26;
        int filled = (int) Math.round((progress / 100.0) * barLength);
        String bar = "█".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, barLength - filled));

        double elapsedSec = context.getElapsedTimeMs() / 1000.0;
        sb.append(String.format("  %sActive Threads:%s %d │ %sDone:%s %d/%d │ %sWaiting:%s %d │ %sFailed:%s %d\n",
                TerminalUI.DIM, TerminalUI.RESET, context.getActiveThreadsCount(),
                TerminalUI.DIM, TerminalUI.RESET, context.getCompletedTasksCount(), agents.size(),
                TerminalUI.DIM, TerminalUI.RESET, context.getPendingWaitingCount(),
                TerminalUI.DIM, TerminalUI.RESET, context.getFailedTasksCount()));

        sb.append(String.format("  %sProgress:%s [%s%s%s] %3d%% │ %sElapsed:%s %.1fs\n",
                TerminalUI.DIM, TerminalUI.RESET,
                TerminalUI.CYAN, bar, TerminalUI.RESET, progress,
                TerminalUI.DIM, TerminalUI.RESET, elapsedSec));
        sb.append(TerminalUI.DIM).append("═".repeat(73)).append(TerminalUI.RESET).append("\n");

        System.out.println(sb);
    }
}
