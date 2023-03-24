package com.sshdaemon.sshd;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.sshdaemon.sshd.SshFingerprint.encode;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintMD5;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintSHA256;
import static com.sshdaemon.util.AndroidLogger.getLogger;
import static com.sshdaemon.util.ExternalStorage.createDirIfNotExists;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.sshdaemon.MainActivity;
import com.sshdaemon.R;
import com.sshdaemon.util.AndroidLogger;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.contrib.server.subsystem.sftp.SimpleAccessControlSftpEventListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/***
 * __     _        ___
 * / _\___| |__    /   \__ _  ___ _ __ ___   ___  _ __
 * \ \/ __| '_ \  / /\ / _` |/ _ \ '_ ` _ \ / _ \| '_ \
 * _\ \__ \ | | |/ /_// (_| |  __/ | | | | | (_) | | | |
 * \__/___/_| |_/___,' \__,_|\___|_| |_| |_|\___/|_| |_|
 */


public class SshDaemon extends Service {

    public static final String AUTHORIZED_KEY_PATH = "/SshDaemon/authorized_keys";
    public static final String CHANNEL_ID = "SshDaemonServiceChannel";
    public static final String SSH_DAEMON = "SshDaemon";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String READ_ONLY = "readOnly";
    private static final Logger logger = getLogger();

    static {
        Security.removeProvider("BC");
        Security.addProvider(new BouncyCastleProvider());
    }

    private SshServer sshd;

    public SshDaemon() {
    }

    public SshDaemon(int port, String user, String password, boolean readOnly) {
        init(port, user, password, readOnly);
    }

    public static boolean publicKeyAuthenticationExists() {
        var authorizedKeyPath = Environment.getExternalStorageDirectory().getPath() + AUTHORIZED_KEY_PATH;
        var authorizedKeyFile = new File(authorizedKeyPath);
        var authorizedKeysExist = false;
        if (authorizedKeyFile.exists()) {
            final var sshPublicKeyAuthenticator = new SshPublicKeyAuthenticator();
            authorizedKeysExist = sshPublicKeyAuthenticator.loadKeysFromPath(authorizedKeyPath);
        }
        return authorizedKeysExist;
    }

    public static Map<SshFingerprint.DIGESTS, String> getFingerPrints() {
        final var result = new HashMap<SshFingerprint.DIGESTS, String>();
        try {
            var rootPath = isNull(Environment.getExternalStorageDirectory()) ? "/" : Environment.getExternalStorageDirectory().getPath();
            var simpleGeneratorHostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get(rootPath + "/" + SSH_DAEMON + "/ssh_host_rsa_key"));
            var keyPairs = simpleGeneratorHostKeyProvider.loadKeys(null);
            final ECPublicKey publicKey = (ECPublicKey) keyPairs.get(0).getPublic();
            final byte[] encodedKey = encode(publicKey);
            result.put(SshFingerprint.DIGESTS.MD5, fingerprintMD5(encodedKey));
            result.put(SshFingerprint.DIGESTS.SHA256, fingerprintSHA256(encodedKey));
        } catch (Exception e) {
            logger.error("Exception while getting fingerprints: ", e);
        }

        return result;
    }

    private void init(int port, String user, String password, boolean readOnly) {
        final var rootPath = Environment.getExternalStorageDirectory().getPath();
        final var path = rootPath + "/" + SSH_DAEMON;
        createDirIfNotExists(path);
        System.setProperty("user.home", rootPath);
        this.sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setPasswordAuthenticator(new SshPasswordAuthenticator(user, password));
        var authorizedKeyPath = rootPath + AUTHORIZED_KEY_PATH;
        var authorizedKeyFile = new File(authorizedKeyPath);
        if (authorizedKeyFile.exists()) {
            final SshPublicKeyAuthenticator sshPublicKeyAuthenticator = new SshPublicKeyAuthenticator();
            sshPublicKeyAuthenticator.loadKeysFromPath(authorizedKeyPath);
            sshd.setPublickeyAuthenticator(sshPublicKeyAuthenticator);
        }
        var simpleGeneratorHostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get(path + "/ssh_host_rsa_key"));
        sshd.setKeyPairProvider(simpleGeneratorHostKeyProvider);
        sshd.setShellFactory(new InteractiveProcessShellFactory());
        var factory = new SftpSubsystemFactory.Builder().build();
        if (readOnly) {
            factory.addSftpEventListener((SimpleAccessControlSftpEventListener.READ_ONLY_ACCESSOR));
        }
        sshd.setSubsystemFactories(Collections.singletonList(factory));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(rootPath)));
        simpleGeneratorHostKeyProvider.loadKeys(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            var serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_HIGH
            );

            var manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);

            var notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
            var pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    0, notificationIntent, FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            var notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setContentTitle(SSH_DAEMON)
                    .setContentText(SSH_DAEMON)
                    .setSmallIcon(R.drawable.play_arrow_fill0_wght400_grad0_opsz48)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);

            var port = intent.getIntExtra(PORT, 8022);
            var user = requireNonNull(intent.getStringExtra(USER), "User should be not null!");
            var password = requireNonNull(intent.getStringExtra(PASSWORD), "Password should be not null!");
            var readOnly = intent.getBooleanExtra(READ_ONLY, false);
            init(port, user, password, readOnly);
            sshd.start();
        } catch (IOException e) {
            AndroidLogger.getLogger().error("Could not start daemon ", e);
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            sshd.stop();
        } catch (IOException e) {
            AndroidLogger.getLogger().error("Could not stop daemon ", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}