package com.sshdaemon.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellFinder {

    private static final Logger logger = LoggerFactory.getLogger(ShellFinder.class);

    // Common shell paths to try on Android
    public static final String[] SHELL_PATHS = {
            "/system/bin/sh",
            "/system/xbin/sh",
            "/vendor/bin/sh",
            "/bin/sh",
            "sh"
    };

    public static String findAvailableShell() {
        for (String shellPath : SHELL_PATHS) {
            try {
                // First check if the shell file exists and is executable
                java.io.File shellFile = new java.io.File(shellPath);
                if (!shellFile.exists()) {
                    logger.debug("Shell {} does not exist", shellPath);
                    continue;
                }
                if (!shellFile.canExecute()) {
                    logger.debug("Shell {} is not executable", shellPath);
                    continue;
                }

                // Test if shell actually works by running a simple command
                ProcessBuilder testPb = new ProcessBuilder(shellPath, "-c", "echo test");
                testPb.redirectErrorStream(true);
                Process testProcess = testPb.start();

                // Wait for the process with a timeout
                boolean finished = testProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (finished && testProcess.exitValue() == 0) {
                    logger.info("Found working shell: {}", shellPath);
                    return shellPath;
                } else {
                    if (!finished) {
                        testProcess.destroyForcibly();
                        logger.debug("Shell {} test timed out", shellPath);
                    } else {
                        logger.debug("Shell {} test failed with exit code: {}", shellPath, testProcess.exitValue());
                    }
                }
            } catch (Exception e) {
                logger.debug("Shell {} test failed: {}", shellPath, e.getMessage());
            }
        }

        // If no standard shell found, log system information for debugging
        logger.warn("No working shell found. System info:");
        logger.warn("  - OS: {}", System.getProperty("os.name", "unknown"));
        logger.warn("  - Arch: {}", System.getProperty("os.arch", "unknown"));
        logger.warn("  - Java version: {}", System.getProperty("java.version", "unknown"));

        return null;
    }

}
