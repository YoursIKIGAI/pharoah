package com.pharoah.test;

import com.pharoah.bridge.PythonAgentBridge;
import com.pharoah.bridge.PythonExecutionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Requirement 11(d): PythonAgentBridge timeout kills sleeping child process.
 */
public class PythonAgentBridgeTimeoutTest {

    @Test
    @DisplayName("ProcessBuilder Timeout: Sleeping Python child process is killed on timeout")
    public void testPythonAgentBridgeTimeoutKillsProcess() {
        // Configure bridge with a strict 2-second timeout
        long timeoutSeconds = 2L;
        PythonAgentBridge bridge = new PythonAgentBridge(timeoutSeconds);

        // Subprocess commands to sleep for 10 seconds (exceeding the 2-second timeout)
        List<String> sleepCommand = List.of(
                bridge.getPythonExecutable(),
                "-c",
                "import time, sys; sys.stdout.write('Child started sleeping...\\n'); sys.stdout.flush(); time.sleep(10); sys.stdout.write('Child finished!\\n');"
        );

        long start = System.currentTimeMillis();
        PythonExecutionResult result = bridge.executeProcess(sleepCommand, "Timeout Test Topic", "SleepWorkflow");
        long elapsed = System.currentTimeMillis() - start;

        // Verify that the process was terminated within ~2-3 seconds rather than running for 10s
        assertTrue(elapsed < 6000, "Process should be terminated promptly after timeout, took: " + elapsed + "ms");
        assertTrue(elapsed >= 1800, "Process should wait for the 2-second timeout threshold, took: " + elapsed + "ms");

        assertNotNull(result);
        assertFalse(result.isSuccess(), "Result should indicate failure due to timeout");
        assertEquals("TIMEOUT", result.getStatus(), "Status should be TIMEOUT");
    }
}
