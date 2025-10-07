package com.sshdaemon.sshd;

import org.apache.sshd.server.channel.ChannelSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Command implementation that executes native system commands
 * This enables rsync and other command-line tools to work over SSH
 */
public class NativeExecuteCommand extends AbstractNativeCommand {
    private static final Logger logger = LoggerFactory.getLogger(NativeExecuteCommand.class);

    private final String command;

    public NativeExecuteCommand(String command, String workingDirectory) {
        super(workingDirectory);
        this.command = command;
    }

    @Override
    protected String getThreadName(ChannelSession channel) {
        return "NativeCommand-" + command.hashCode();
    }

    @Override
    public void run() {
        try {
            logger.info("Executing command: {}", command);

            // Find available shell using shared utility
            String shellPath = findShellOrExit();
            if (shellPath == null) {
                return; // Error already handled by base class
            }

            // Create process builder to execute the command via shell
            ProcessBuilder pb = new ProcessBuilder(shellPath, "-c", command);
            pb.directory(new java.io.File(workingDirectory));
            pb.redirectErrorStream(false); // Keep stderr separate for proper rsync protocol

            // Set up environment using shared functionality
            setupEnvironment(pb, shellPath);

            // Start the process
            process = pb.start();
            logger.info("Command process started: {}", command);

            // Create I/O threads using shared functionality (8KB buffer for rsync)
            createSimpleIOThreads(8192);

            // Wait for completion using shared functionality
            int exitCode = waitForCompletion(2000);

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
}
