package com.sshdaemon.sshd;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple terminal emulator that handles basic ANSI escape sequences and terminal features
 */
public class TerminalEmulator {
    
    // ANSI escape sequences
    public static final String RESET = "\u001b[0m";
    public static final String CLEAR_SCREEN = "\u001b[2J\u001b[H";
    public static final String CLEAR_LINE = "\u001b[2K";
    public static final String CURSOR_UP = "\u001b[A";
    public static final String CURSOR_DOWN = "\u001b[B";
    public static final String CURSOR_RIGHT = "\u001b[C";
    public static final String CURSOR_LEFT = "\u001b[D";
    
    // Colors
    public static final String BLACK = "\u001b[30m";
    public static final String RED = "\u001b[31m";
    public static final String GREEN = "\u001b[32m";
    public static final String YELLOW = "\u001b[33m";
    public static final String BLUE = "\u001b[34m";
    public static final String MAGENTA = "\u001b[35m";
    public static final String CYAN = "\u001b[36m";
    public static final String WHITE = "\u001b[37m";
    
    // Bright colors
    public static final String BRIGHT_BLACK = "\u001b[90m";
    public static final String BRIGHT_RED = "\u001b[91m";
    public static final String BRIGHT_GREEN = "\u001b[92m";
    public static final String BRIGHT_YELLOW = "\u001b[93m";
    public static final String BRIGHT_BLUE = "\u001b[94m";
    public static final String BRIGHT_MAGENTA = "\u001b[95m";
    public static final String BRIGHT_CYAN = "\u001b[96m";
    public static final String BRIGHT_WHITE = "\u001b[97m";
    
    private final OutputStream outputStream;
    private final Map<String, String> environment;
    private int columns = 80;
    private int rows = 24;
    
    public TerminalEmulator(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.environment = new HashMap<>();
        initializeEnvironment();
    }
    
    private void initializeEnvironment() {
        environment.put("TERM", "xterm-256color");
        environment.put("COLUMNS", String.valueOf(columns));
        environment.put("LINES", String.valueOf(rows));
        environment.put("PATH", "/system/bin:/system/xbin");
        environment.put("HOME", System.getProperty("user.home", "/"));
        environment.put("USER", System.getProperty("user.name", "android"));
        environment.put("SHELL", "/system/bin/sh");
        environment.put("ANDROID_SSH", "1");
    }
    
    public void setTerminalSize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        environment.put("COLUMNS", String.valueOf(columns));
        environment.put("LINES", String.valueOf(rows));
    }
    
    public int getColumns() {
        return columns;
    }
    
    public int getRows() {
        return rows;
    }
    
    public Map<String, String> getEnvironment() {
        return new HashMap<>(environment);
    }
    
    public void write(String text) throws IOException {
        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
    
    public void writeLine(String text) throws IOException {
        write(text + "\r\n");
    }
    
    public void writeError(String text) throws IOException {
        write(RED + text + RESET);
    }
    
    public void writeSuccess(String text) throws IOException {
        write(GREEN + text + RESET);
    }
    
    public void writeWarning(String text) throws IOException {
        write(YELLOW + text + RESET);
    }
    
    public void writeInfo(String text) throws IOException {
        write(CYAN + text + RESET);
    }
    
    public void clearScreen() throws IOException {
        write(CLEAR_SCREEN);
    }
    
    public void clearLine() throws IOException {
        write(CLEAR_LINE);
    }
    
    public void moveCursor(int row, int col) throws IOException {
        write(String.format("\u001b[%d;%dH", row, col));
    }
    
    public void moveCursorUp(int lines) throws IOException {
        write(String.format("\u001b[%dA", lines));
    }
    
    public void moveCursorDown(int lines) throws IOException {
        write(String.format("\u001b[%dB", lines));
    }
    
    public void moveCursorRight(int columns) throws IOException {
        write(String.format("\u001b[%dC", columns));
    }
    
    public void moveCursorLeft(int columns) throws IOException {
        write(String.format("\u001b[%dD", columns));
    }
    
    public void saveCursor() throws IOException {
        write("\u001b[s");
    }
    
    public void restoreCursor() throws IOException {
        write("\u001b[u");
    }
    
    public void hideCursor() throws IOException {
        write("\u001b[?25l");
    }
    
    public void showCursor() throws IOException {
        write("\u001b[?25h");
    }
    
    public void enableAlternateScreen() throws IOException {
        write("\u001b[?1049h");
    }
    
    public void disableAlternateScreen() throws IOException {
        write("\u001b[?1049l");
    }
    
    public void bold() throws IOException {
        write("\u001b[1m");
    }
    
    public void dim() throws IOException {
        write("\u001b[2m");
    }
    
    public void underline() throws IOException {
        write("\u001b[4m");
    }
    
    public void blink() throws IOException {
        write("\u001b[5m");
    }
    
    public void reverse() throws IOException {
        write("\u001b[7m");
    }
    
    public void reset() throws IOException {
        write(RESET);
    }
    
    /**
     * Format file permissions in Unix style (e.g., drwxr-xr-x)
     */
    public static String formatPermissions(boolean isDirectory, boolean canRead, boolean canWrite, boolean canExecute) {
        StringBuilder perms = new StringBuilder();
        perms.append(isDirectory ? 'd' : '-');
        perms.append(canRead ? 'r' : '-');
        perms.append(canWrite ? 'w' : '-');
        perms.append(canExecute ? 'x' : '-');
        perms.append("r--r--"); // Owner, group, others (simplified for Android)
        return perms.toString();
    }
    
    /**
     * Format file size in human-readable format
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Create a colored prompt based on current directory and user
     */
    public String createPrompt(String user, String currentDir, String rootDir) {
        StringBuilder prompt = new StringBuilder();
        
        // User in green
        prompt.append(GREEN).append(user).append(RESET);
        prompt.append("@");
        
        // Host in cyan
        prompt.append(CYAN).append("android").append(RESET);
        prompt.append(":");
        
        // Current directory in blue
        String displayDir = currentDir;
        if (currentDir.equals(rootDir)) {
            displayDir = "~";
        } else if (currentDir.startsWith(rootDir)) {
            displayDir = "~" + currentDir.substring(rootDir.length());
        }
        prompt.append(BLUE).append(displayDir).append(RESET);
        
        prompt.append("$ ");
        
        return prompt.toString();
    }
}
