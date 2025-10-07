package com.sshdaemon.sshd;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Command implementation that executes native system commands
 * This enables rsync and other command-line tools to work over SSH
 */
public class NativeExecuteCommand implements Command, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(NativeExecuteCommand.class);

    private final String command;
    private final String workingDirectory;

    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Environment environment;
    private Thread commandThread;
    private Process commandProcess;

    // Common shell paths to try on Android
    private static final String[] SHELL_PATHS = {
            "/system/bin/sh",
            "/system/xbin/sh",
            "/vendor/bin/sh",
            "/bin/sh",
            "sh"
    };

    public NativeExecuteCommand(String command, String workingDirectory) {
        this.command = command;
        this.workingDirectory = workingDirectory != null ? workingDirectory : "/";
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(ChannelSession channel, Environment env) throws IOException {
        this.environment = env;
        this.commandThread = new Thread(this, "NativeCommand-" + command.hashCode());
        this.commandThread.start();
    }

    @Override
    public void destroy(ChannelSession channel) {
        if (commandProcess != null) {
            commandProcess.destroy();
        }
        if (commandThread != null) {
            commandThread.interrupt();
        }
    }

    @Override
    public void run() {
        try {
            logger.info("Executing command: {}", command);

            // Find available shell
            String shellPath = findAvailableShell();
            if (shellPath == null) {
                logger.error("No working shell found for command execution");
                writeError("ERROR: No working shell found on this Android device.\r\n");
                callback.onExit(127); // Command not found
                return;
            }

            // Create process builder to execute the command via shell
            ProcessBuilder pb = new ProcessBuilder(shellPath, "-c", command);
            pb.directory(new java.io.File(workingDirectory));
            pb.redirectErrorStream(false); // Keep stderr separate for proper rsync protocol

            // Set environment variables
            Map<String, String> processEnv = pb.environment();

            // Copy SSH environment variables
            if (environment != null) {
                for (Map.Entry<String, String> entry : environment.getEnv().entrySet()) {
                    processEnv.put(entry.getKey(), entry.getValue());
                }
            }

            // Set common shell environment variables
            processEnv.put("HOME", workingDirectory);
            processEnv.put("PWD", workingDirectory);
            processEnv.put("SHELL", shellPath);
            processEnv.put("TERM", processEnv.getOrDefault("TERM", "xterm-256color"));
            processEnv.put("USER", processEnv.getOrDefault("USER", "android"));
            processEnv.put("LANG", processEnv.getOrDefault("LANG", "en_US.UTF-8"));

            // Set comprehensive PATH for Android including common tool locations
            // Use app-writable directories first, then system directories
            String appBinDir = workingDirectory + "/bin";
            String defaultPath = appBinDir + ":/system/bin:/system/xbin:/vendor/bin:/data/local/tmp:/sbin:/data/data/com.termux/files/usr/bin:/data/data/com.sshdaemon/files/usr/bin";
            String existingPath = processEnv.get("PATH");
            if (existingPath != null && !existingPath.isEmpty()) {
                processEnv.put("PATH", existingPath + ":" + defaultPath);
            } else {
                processEnv.put("PATH", defaultPath);
            }

            // Create bin directory in working directory if it doesn't exist
            try {
                java.io.File binDir = new java.io.File(appBinDir);
                if (!binDir.exists()) {
                    binDir.mkdirs();
                    logger.info("Created app bin directory: {}", appBinDir);
                }
            } catch (Exception e) {
                logger.debug("Could not create app bin directory: {}", e.getMessage());
            }

            // Add Android-specific environment variables
            processEnv.put("ANDROID_DATA", "/data");
            processEnv.put("ANDROID_ROOT", "/system");
            processEnv.put("EXTERNAL_STORAGE", "/sdcard");

            // Start the process
            commandProcess = pb.start();
            logger.info("Command process started: {}", command);

            // Create threads to handle I/O
            Thread inputThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[8192]; // Larger buffer for rsync
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (commandProcess != null && commandProcess.isAlive()) {
                            commandProcess.getOutputStream().write(buffer, 0, bytesRead);
                            commandProcess.getOutputStream().flush();
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.debug("Input stream closed: {}", e.getMessage());
                } finally {
                    try {
                        if (commandProcess != null && commandProcess.isAlive()) {
                            commandProcess.getOutputStream().close();
                        }
                    } catch (IOException e) {
                        logger.debug("Error closing process input stream: {}", e.getMessage());
                    }
                }
            }, "CommandInput");

            Thread outputThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[8192]; // Larger buffer for rsync
                    int bytesRead;
                    while ((bytesRead = commandProcess.getInputStream().read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        out.flush();
                    }
                } catch (IOException e) {
                    logger.debug("Output stream closed: {}", e.getMessage());
                }
            }, "CommandOutput");

            Thread errorThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[8192]; // Larger buffer for rsync
                    int bytesRead;
                    while ((bytesRead = commandProcess.getErrorStream().read(buffer)) != -1) {
                        err.write(buffer, 0, bytesRead);
                        err.flush();
                    }
                } catch (IOException e) {
                    logger.debug("Error stream closed: {}", e.getMessage());
                }
            }, "CommandError");

            // Start I/O threads
            inputThread.setDaemon(true);
            outputThread.setDaemon(true);
            errorThread.setDaemon(true);

            inputThread.start();
            outputThread.start();
            errorThread.start();

            // Wait for process to complete
            int exitCode = commandProcess.waitFor();

            // Wait a bit for I/O threads to finish
            try {
                outputThread.join(2000);
                errorThread.join(2000);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while waiting for I/O threads");
            }

            logger.info("Command completed with exit code: {} - {}", exitCode, command);
            callback.onExit(exitCode);

        } catch (Exception e) {
            logger.error("Error executing command: " + command, e);
            try {
                writeError("Command execution error: " + e.getMessage() + "\r\n");
            } catch (IOException ioException) {
                logger.error("Failed to write error message", ioException);
            }
            callback.onExit(1);
        }
    }

    private String findAvailableShell() {
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
                    logger.debug("Found working shell for command execution: {}", shellPath);
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
        return null;
    }

    private void writeError(String message) throws IOException {
        err.write(message.getBytes());
        err.flush();
    }
}
