package com.sshdaemon.sshd;

import static java.util.Objects.requireNonNull;

import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;

public class SshPasswordAuthenticator implements PasswordAuthenticator {

    private final String user;
    private final String password;

    public SshPasswordAuthenticator(String user, String password) {
        this.user = requireNonNull(user);
        this.password = requireNonNull(password);
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException, AsyncAuthException {
        return username.equals(user) && password.equals(this.password);
    }
}