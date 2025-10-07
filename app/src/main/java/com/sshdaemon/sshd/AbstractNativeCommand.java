package com.sshdaemon.sshd;

import static com.sshdaemon.util.ShellFinder.findAvailableShell;

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
 * Abstract base class for native Android command implementations
 * Contains shared functionality for environment setup and process management
 */
public abstract class AbstractNativeCommand implements Command, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNativeCommand.class);

    protected final String workingDirectory;

    protected InputStream in;
    protected OutputStream out;
    protected OutputStream err;
    protected ExitCallback callback;
    protected Environment environment;
    protected Thread commandThread;
    protected Process process;

    public AbstractNativeCommand(String workingDirectory) {
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
        this.commandThread = new Thread(this, getThreadName(channel));
        this.commandThread.start();
    }

    @Override
    public void destroy(ChannelSession channel) {
        if (process != null) {
            process.destroy();
        }
        if (commandThread != null) {
            commandThread.interrupt();
        }
    }

    /**
     * Sets up common environment variables for Android shell processes
     */
    protected void setupEnvironment(ProcessBuilder pb, String shellPath) {
        Map<String, String> processEnv = pb.environment();

        // Copy SSH environment variables
        if (environment != null) {
            processEnv.putAll(environment.getEnv());
        }

        // Set common shell environment variables
        processEnv.put("HOME", workingDirectory);
        processEnv.put("PWD", workingDirectory);
        processEnv.put("SHELL", shellPath);
        processEnv.put("TERM", processEnv.getOrDefault("TERM", "xterm-256color"));
        processEnv.put("USER", processEnv.getOrDefault("USER", "android"));
        processEnv.put("LANG", processEnv.getOrDefault("LANG", "en_US.UTF-8"));

        // Set comprehensive PATH for Android including common tool locations
        String appBinDir = workingDirectory + "/bin";
        String defaultPath = appBinDir + ":/system/bin:/system/xbin:/vendor/bin:/data/local/tmp:/sbin:/data/data/com.termux/files/usr/bin:/data/data/com.sshdaemon/files/usr/bin";
        String existingPath = processEnv.get("PATH");
        if (existingPath != null && !existingPath.isEmpty()) {
            processEnv.put("PATH", existingPath + ":" + defaultPath);
        } else {
            processEnv.put("PATH", defaultPath);
        }

        processEnv.put("ANDROID_DATA", "/data");
        processEnv.put("ANDROID_ROOT", "/system");
        processEnv.put("EXTERNAL_STORAGE", "/sdcard");
    }

    /**
     * Finds available shell or handles error if none found
     */
    protected String findShellOrExit() {
        String shellPath = findAvailableShell();
        if (shellPath == null) {
            logger.error("No working shell found for command execution");
            try {
                writeError("ERROR: No working shell found on this Android device.\r\n");
                writeError("This may be due to:\r\n");
                writeError("1. Restricted Android environment\r\n");
                writeError("2. Missing shell binaries\r\n");
                writeError("3. Permission restrictions\r\n");
                writeError("\r\nPlease check device configuration or contact administrator.\r\n");
            } catch (IOException e) {
                logger.error("Failed to write error message", e);
            }
            callback.onExit(127); // Command not found
            return null;
        }
        return shellPath;
    }

    /**
     * Creates standard I/O threads for simple command execution (non-interactive)
     */
    protected void createSimpleIOThreads(int bufferSize) {
        Thread inputThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    if (process != null && process.isAlive()) {
                        process.getOutputStream().write(buffer, 0, bytesRead);
                        process.getOutputStream().flush();
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                logger.debug("Input stream closed: {}", e.getMessage());
            } finally {
                try {
                    if (process != null && process.isAlive()) {
                        process.getOutputStream().close();
                    }
                } catch (IOException e) {
                    logger.debug("Error closing process input stream: {}", e.getMessage());
                }
            }
        }, "Input");

        Thread outputThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
            } catch (IOException e) {
                logger.debug("Output stream closed: {}", e.getMessage());
            }
        }, "Output");

        Thread errorThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = process.getErrorStream().read(buffer)) != -1) {
                    err.write(buffer, 0, bytesRead);
                    err.flush();
                }
            } catch (IOException e) {
                logger.debug("Error stream closed: {}", e.getMessage());
            }
        }, "Error");

        // Start I/O threads
        inputThread.setDaemon(true);
        outputThread.setDaemon(true);
        errorThread.setDaemon(true);

        inputThread.start();
        outputThread.start();
        errorThread.start();
    }

    /**
     * Waits for process completion and I/O threads to finish
     */
    protected int waitForCompletion(int ioTimeout) throws InterruptedException {
        int exitCode = process.waitFor();

        // Wait for I/O threads to finish
        Thread[] threads = Thread.getAllStackTraces().keySet().toArray(new Thread[0]);
        for (Thread thread : threads) {
            if (thread.getName().equals("Input") || thread.getName().equals("Output") || thread.getName().equals("Error")) {
                try {
                    thread.join(ioTimeout);
                } catch (InterruptedException e) {
                    logger.debug("Interrupted while waiting for I/O thread: {}", thread.getName());
                }
            }
        }

        return exitCode;
    }

    protected void writeError(String message) throws IOException {
        err.write(message.getBytes());
        err.flush();
    }

    /**
     * Get thread name for this command type
     */
    protected abstract String getThreadName(ChannelSession channel);

    /**
     * Run the specific command implementation
     */
    @Override
    public abstract void run();
}
