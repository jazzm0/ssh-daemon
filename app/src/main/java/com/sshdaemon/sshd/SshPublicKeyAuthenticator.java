package com.sshdaemon.sshd;

import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;

public class SshPublicKeyAuthenticator implements PublickeyAuthenticator {

    public SshPublicKeyAuthenticator() {
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) throws AsyncAuthException {
        return false;
    }
}
