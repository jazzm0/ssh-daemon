package com.sshdaemon.sshd;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Factory that creates native Android shell sessions
 */
public class NativeShellFactory implements ShellFactory {
    private static final Logger logger = LoggerFactory.getLogger(NativeShellFactory.class);
    
    private final String workingDirectory;
    private final boolean readOnly;
    
    public NativeShellFactory(String workingDirectory, boolean readOnly) {
        this.workingDirectory = workingDirectory;
        this.readOnly = readOnly;
    }
    
    @Override
    public Command createShell(ChannelSession channelSession) throws IOException {
        logger.debug("Creating native shell session for channel: {}", channelSession);
        return new NativeShellCommand(workingDirectory, readOnly);
    }
}
