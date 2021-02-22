package com.sshdaemon.sshd;

import com.sshdaemon.MainActivity;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sshdaemon.util.ExternalStorage.createDirIfNotExists;

/***
 * __     _        ___
 * / _\___| |__    /   \__ _  ___ _ __ ___   ___  _ __
 * \ \/ __| '_ \  / /\ / _` |/ _ \ '_ ` _ \ / _ \| '_ \
 * _\ \__ \ | | |/ /_// (_| |  __/ | | | | | (_) | | | |
 * \__/___/_| |_/___,' \__,_|\___|_| |_| |_|\___/|_| |_|
 */


public class SshDaemon {

    private final SshServer sshd;
    private final List<KeyPair> keyPairs;

    public SshDaemon(String rootPath, int port, String user, String password) {
        final String path = rootPath + "/" + "SshDaemon";
        createDirIfNotExists(path);
        System.setProperty("user.home", rootPath);
        this.sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setPasswordAuthenticator(new SshPasswordAuthenticator(user, password));
        String authorizedKeyPath = path + "/authorized_keys";
        File authorizedKeyFile = new File(authorizedKeyPath);
        if (authorizedKeyFile.exists()) {
            final SshPublicKeyAuthenticator sshPublicKeyAuthenticator = new SshPublicKeyAuthenticator();
            sshPublicKeyAuthenticator.loadKeysFromPath(authorizedKeyPath);
            sshd.setPublickeyAuthenticator(sshPublicKeyAuthenticator);
        }
        SimpleGeneratorHostKeyProvider simpleGeneratorHostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get(path + "/ssh_host_rsa_key"));
        sshd.setKeyPairProvider(simpleGeneratorHostKeyProvider);
        sshd.setShellFactory(new InteractiveProcessShellFactory());
        SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder().build();
        sshd.setSubsystemFactories(Collections.singletonList(factory));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(rootPath)));
        this.keyPairs = simpleGeneratorHostKeyProvider.loadKeys(null);
    }

    public Map<SshFingerprint.DIGESTS, String> getFingerPrints() throws NoSuchAlgorithmException {
        final Map<SshFingerprint.DIGESTS, String> result = new HashMap<>();
        final RSAPublicKey publicKey = (RSAPublicKey) this.keyPairs.get(0).getPublic();

        result.put(SshFingerprint.DIGESTS.MD5, SshFingerprint.fingerprintMD5(publicKey.getPublicExponent(), publicKey.getModulus()));
        result.put(SshFingerprint.DIGESTS.SHA256, SshFingerprint.fingerprintSHA256(publicKey.getPublicExponent(), publicKey.getModulus()));

        return result;
    }

    public void start() throws IOException {
        sshd.start();
    }

    public void stop() throws IOException {
        sshd.stop();
    }

    public boolean hasPublicKeyAuthentication() {
        return !this.keyPairs.isEmpty();
    }

    public boolean isRunning() {
        return sshd.isStarted();
    }
}