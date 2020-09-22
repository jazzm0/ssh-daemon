package com.sshdaemon.sshd;

import android.os.Environment;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;

import java.io.IOException;

import static com.sshdaemon.util.ExternalStorage.getSdCardPath;

/***
 * __     _        ___
 * / _\___| |__    /   \__ _  ___ _ __ ___   ___  _ __
 * \ \/ __| '_ \  / /\ / _` |/ _ \ '_ ` _ \ / _ \| '_ \
 * _\ \__ \ | | |/ /_// (_| |  __/ | | | | | (_) | | | |
 * \__/___/_| |_/___,' \__,_|\___|_| |_| |_|\___/|_| |_|
 */


public class SshDaemon {

    public static final int DEFAULT_PORT = 8022;

    private final SshServer sshd;

    public SshDaemon(int port) throws IOException {
        System.setProperty("user.home", getSdCardPath());
        this.sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setPasswordAuthenticator(new SshPasswordAuthenticator());
        sshd.setPublickeyAuthenticator(new SshPublicKeyAuthenticator());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Environment.getExternalStorageDirectory().getAbsoluteFile().toPath()));
        sshd.setShellFactory(new InteractiveProcessShellFactory());
        sshd.start();
    }
}