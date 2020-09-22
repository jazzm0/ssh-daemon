package com.sshdaemon.sshd;

import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;

public class SshPasswordAuthenticator implements PasswordAuthenticator {

    private static final String TAG = "PasswordAuthenticator";
    private static SshPasswordAuthenticator instance = new SshPasswordAuthenticator();

    private final String user = "user";
    private final String password = "password";

    public SshPasswordAuthenticator() {
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException, AsyncAuthException {
        return username.equals(user) && password.equals(this.password);
    }
}