package com.sshdaemon.sshd;

import android.os.Environment;

import com.sshdaemon.util.ExternalStorage;

import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static com.sshdaemon.util.ExternalStorage.getSdCardPath;

/***
 * __     _        ___
 * / _\___| |__    /   \__ _  ___ _ __ ___   ___  _ __
 * \ \/ __| '_ \  / /\ / _` |/ _ \ '_ ` _ \ / _ \| '_ \
 * _\ \__ \ | | |/ /_// (_| |  __/ | | | | | (_) | | | |
 * \__/___/_| |_/___,' \__,_|\___|_| |_| |_|\___/|_| |_|
 */


public class SshDaemon {

    private final SshServer sshd;

    public SshDaemon(int port) throws IOException {
        System.setProperty("user.home", getSdCardPath());
        this.sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setPasswordAuthenticator(new SshPasswordAuthenticator());
        sshd.setPublickeyAuthenticator(new SshPublicKeyAuthenticator());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Environment.getDataDirectory().toPath()));
        sshd.setShellFactory(new InteractiveProcessShellFactory());
        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
//        sshd.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(getSdCardPath())));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(Environment.getExternalStorageDirectory().toPath()));
        SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder().build();
        sshd.setSubsystemFactories(Collections.singletonList(factory));
        sshd.start();
    }
}