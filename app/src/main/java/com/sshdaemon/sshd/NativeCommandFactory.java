package com.sshdaemon.sshd;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command factory that creates commands for execution via SSH
 * This enables rsync and other command-line tools to work over SSH
 */
public class NativeCommandFactory implements CommandFactory {
    private static final Logger logger = LoggerFactory.getLogger(NativeCommandFactory.class);

    private final String workingDirectory;
    private final boolean readOnly;

    public NativeCommandFactory(String workingDirectory, boolean readOnly) {
        this.workingDirectory = workingDirectory;
        this.readOnly = readOnly;
    }

    @Override
    public Command createCommand(ChannelSession channelSession, String command) {
        logger.info("Creating command: {}", command);

        // Return a native command that will execute the requested command
        return new NativeExecuteCommand(command, workingDirectory, readOnly);
    }
}
