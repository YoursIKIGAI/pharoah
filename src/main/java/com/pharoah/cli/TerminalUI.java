package com.pharoah.cli;

import com.pharoah.config.AppConfig;

// VARIJ: Terminal UI Formatter.
// Provides elegant ANSI colors, consistent box-drawing borders,
// clear breadcrumbs, and standardized status badges for terminal presentation.
public class TerminalUI {

    private static final boolean ANSI_SUPPORTED = AppConfig.getInstance().isAnsiColorsEnabled();

    // ANSI Color Codes
    public static final String RESET   = ANSI_SUPPORTED ? "\u001B[0m"  : "";
    public static final String BOLD    = ANSI_SUPPORTED ? "\u001B[1m"  : "";
    public static final String DIM     = ANSI_SUPPORTED ? "\u001B[2m"  : "";
    public static final String CYAN    = ANSI_SUPPORTED ? "\u001B[36m" : "";
    public static final String GREEN   = ANSI_SUPPORTED ? "\u001B[32m" : "";
    public static final String YELLOW  = ANSI_SUPPORTED ? "\u001B[33m" : "";
    public static final String RED     = ANSI_SUPPORTED ? "\u001B[31m" : "";
    public static final String MAGENTA = ANSI_SUPPORTED ? "\u001B[35m" : "";
    public static final String BLUE    = ANSI_SUPPORTED ? "\u001B[34m" : "";
    public static final String WHITE   = ANSI_SUPPORTED ? "\u001B[37m" : "";

    // Status Badges
    public static final String BADGE_SUCCESS = GREEN + "[✓] " + RESET;
    public static final String BADGE_RUNNING = CYAN  + "[⏳] " + RESET;
    public static final String BADGE_WAITING = YELLOW + "[!] " + RESET;
    public static final String BADGE_FAILED  = RED   + "[✗] " + RESET;
    public static final String BADGE_INFO    = BLUE  + "[ℹ] " + RESET;

    public static void printBanner(String version, String dbEngine, boolean dbConnected) {
        String statusText = dbConnected ? GREEN + "CONNECTED" + RESET : YELLOW + "SQLITE FALLBACK" + RESET;
        System.out.println(CYAN + BOLD);
        System.out.println("  ╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("  ║                               PHAROAH                                 ║");
        System.out.println("  ║            Multi-Agent Orchestration & Knowledge System               ║");
        System.out.println("  ╚═══════════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.printf("   %sEngine:%s Java 21 LTS | %sDB:%s %s (%s) | %sInterface:%s Terminal\n",
                DIM, RESET, DIM, RESET, dbEngine, statusText, DIM, RESET);
        System.out.println(DIM + "   " + "─".repeat(67) + RESET);
    }

    public static void printBreadcrumb(String... path) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ").append(CYAN).append("PHAROAH").append(RESET);
        for (int i = 0; i < path.length; i++) {
            sb.append(" ").append(DIM).append("❯").append(RESET).append(" ");
            if (i == path.length - 1) {
                sb.append(BOLD).append(WHITE).append(path[i]).append(RESET);
            } else {
                sb.append(path[i]);
            }
        }
        System.out.println("\n" + sb + "\n");
    }

    public static void printSectionHeader(String title) {
        System.out.println(CYAN + " ┌── " + BOLD + title + RESET);
    }

    public static void printSectionFooter() {
        System.out.println(CYAN + " └" + "─".repeat(50) + RESET + "\n");
    }

    public static void printMenuOption(String key, String title, String description) {
        System.out.printf("  %s%s%2s%s %s%-24s%s %s%s%s\n",
                BOLD, CYAN, key, RESET,
                WHITE, title, RESET,
                DIM, description, RESET);
    }

    public static void printSuccess(String message) {
        System.out.println("  " + BADGE_SUCCESS + GREEN + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println("  " + BADGE_INFO + message);
    }

    public static void printWarning(String message) {
        System.out.println("  " + BADGE_WAITING + YELLOW + message + RESET);
    }

    public static void printError(String message) {
        System.out.println("  " + BADGE_FAILED + RED + message + RESET);
    }

    public static void printDivider() {
        System.out.println(DIM + " " + "─".repeat(67) + RESET);
    }
}
