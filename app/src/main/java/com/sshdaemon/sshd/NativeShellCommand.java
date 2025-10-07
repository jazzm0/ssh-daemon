package com.sshdaemon.sshd;

import org.apache.sshd.server.channel.ChannelSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Native shell command that provides access to the Android system shell
 */
public class NativeShellCommand extends AbstractNativeCommand {
    private static final Logger logger = LoggerFactory.getLogger(NativeShellCommand.class);

    private TerminalEmulator terminal;

    public NativeShellCommand(String workingDirectory) {
        super(workingDirectory);
    }

    @Override
    protected String getThreadName(ChannelSession channel) {
        return "NativeShell-" + channel.toString();
    }


    @Override
    public void run() {
        try {
            // Find available shell using shared utility
            String shellPath = findShellOrExit();
            if (shellPath == null) {
                return; // Error already handled by base class
            }

            logger.info("Starting native shell: {}", shellPath);

            // Create process builder - use non-interactive shell to avoid TTY issues on Android
            ProcessBuilder pb = new ProcessBuilder(shellPath);
            pb.directory(new java.io.File(workingDirectory));
            pb.redirectErrorStream(true); // Merge stderr with stdout for simplicity

            // Set up environment using shared functionality
            setupEnvironment(pb, shellPath);

            // Initialize terminal emulator
            terminal = new TerminalEmulator(out);

            // Set terminal size from SSH environment if available
            if (environment != null) {
                Map<String, String> env = environment.getEnv();
                try {
                    int cols = Integer.parseInt(Objects.requireNonNull(env.getOrDefault("COLUMNS", "80")));
                    int rows = Integer.parseInt(Objects.requireNonNull(env.getOrDefault("LINES", "24")));
                    terminal.setTerminalSize(cols, rows);
                } catch (NumberFormatException e) {
                    // Use defaults
                }
            }

            process = pb.start();

            terminal.write("Current directory: " + workingDirectory + "\r\n");
            String prompt = terminal.createPrompt("android", workingDirectory, "/");
            terminal.write(prompt);

            // Create threads to handle I/O
            Thread inputThread = new Thread(() -> {
                try {
                    logger.info("InputThread started, waiting for input...");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        logger.info("InputThread: received {} bytes from SSH client", bytesRead);

                        // Echo the input back to the client so they can see what they're typing
                        String input = new String(buffer, 0, bytesRead);

                        // Handle special characters
                        if (input.equals("\r") || input.equals("\n") || input.equals("\r\n")) {
                            // Enter pressed - send command to shell and newline to client
                            terminal.write("\r\n");
                            if (process != null && process.isAlive()) {
                                process.getOutputStream().write("\n".getBytes());
                                process.getOutputStream().flush();
                                logger.info("InputThread: sent newline to shell process");
                            }
                        } else if (input.equals("\u0008") || input.equals("\u007f")) {
                            // Backspace - handle locally
                            terminal.write("\b \b"); // Backspace, space, backspace
                        } else {
                            // Regular character - echo to client and send to shell
                            terminal.write(input);
                            if (process != null && process.isAlive()) {
                                process.getOutputStream().write(buffer, 0, bytesRead);
                                process.getOutputStream().flush();
                                logger.info("InputThread: sent {} bytes to shell process", bytesRead);
                            } else {
                                logger.error("InputThread: shell process is not alive!");
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error("InputThread error: {}", e.getMessage());
                } finally {
                    logger.info("InputThread ending");
                    try {
                        if (process != null && process.isAlive()) {
                            process.getOutputStream().close();
                        }
                    } catch (IOException e) {
                        logger.debug("Error closing process output stream: {}", e.getMessage());
                    }
                }
            }, "ShellInput");

            Thread outputThread = new Thread(() -> {
                try {
                    logger.info("OutputThread started, waiting for shell output...");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    StringBuilder commandBuffer = new StringBuilder();
                    boolean waitingForPrompt = false;

                    while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                        logger.info("OutputThread: received {} bytes from shell", bytesRead);
                        String output = new String(buffer, 0, bytesRead);

                        commandBuffer.append(output);

                        if (output.endsWith("\n") || output.endsWith("\r\n")) {
                            String fullOutput = commandBuffer.toString();

                            // Don't echo empty lines or prompt-like output
                            if (!fullOutput.trim().isEmpty() &&
                                    !fullOutput.trim().equals("$") &&
                                    !fullOutput.matches("^\\s*$")) {

                                // Ensure proper line endings for terminal compatibility
                                String cleanOutput = fullOutput.replace("\n", "\r\n");
                                // Write the accumulated output directly to out stream for better terminal compatibility
                                out.write(cleanOutput.getBytes());
                                out.flush();
                                waitingForPrompt = true;
                            }

                            // Clear the buffer
                            commandBuffer.setLength(0);

                            // Add our custom prompt after command output
                            if (waitingForPrompt) {
                                String newPrompt = terminal.createPrompt("android", workingDirectory, "/");
                                terminal.write(newPrompt);
                                waitingForPrompt = false;
                            }
                        } else {
                            // For partial output (no newline yet), just write it directly
                            // This handles interactive programs that don't end with newlines
                            if (commandBuffer.length() > 0) {
                                // Write directly to output stream with proper line endings
                                String cleanOutput = output.replace("\n", "\r\n");
                                out.write(cleanOutput.getBytes());
                                out.flush();
                                commandBuffer.setLength(0); // Clear since we wrote it
                            }
                        }

                        logger.info("OutputThread: processed and sent output to SSH client");
                    }
                } catch (IOException e) {
                    logger.error("OutputThread error: {}", e.getMessage());
                } finally {
                    logger.info("OutputThread ending");
                }
            }, "ShellOutput");

            Thread errorThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getErrorStream().read(buffer)) != -1) {
                        err.write(buffer, 0, bytesRead);
                        err.flush();
                    }
                } catch (IOException e) {
                    logger.debug("Error stream closed: {}", e.getMessage());
                }
            }, "ShellError");

            // Start I/O threads
            inputThread.setDaemon(true);
            outputThread.setDaemon(true);
            errorThread.setDaemon(true);

            inputThread.start();
            outputThread.start();
            errorThread.start();

            // Check if shell process is alive and send initial commands
            logger.info("Shell process started");
            logger.info("Shell process alive: {}", process.isAlive());

            try {
                Thread.sleep(200); // Give shell time to initialize

                if (process.isAlive()) {
                    // Set up shell environment - disable shell echo since we handle it
                    String initCommands = "stty -echo 2>/dev/null || true\nexport PS1=''\n";
                    process.getOutputStream().write(initCommands.getBytes());
                    process.getOutputStream().flush();
                    logger.info("Sent initial commands to shell");

                    // Give time for initial commands to process
                    Thread.sleep(100);
                    logger.info("Shell process still alive after init: {}", process.isAlive());
                } else {
                    logger.error("Shell process died immediately after start!");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Wait for process to complete
            int exitCode = process.waitFor();

            // Wait a bit for I/O threads to finish
            try {
                outputThread.join(1000);
                errorThread.join(1000);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while waiting for I/O threads");
            }

            logger.info("Shell process exited with code: {}", exitCode);
            callback.onExit(exitCode);

        } catch (Exception e) {
            logger.error("Error in shell execution", e);
            try {
                writeError("Shell error: " + e.getMessage() + "\r\n");
            } catch (IOException ioException) {
                logger.error("Failed to write error message", ioException);
            }
            callback.onExit(1);
        }
    }


}
