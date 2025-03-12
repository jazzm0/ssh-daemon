package com.sshdaemon.sshd;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintMD5;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintSHA256;
import static com.sshdaemon.util.AndroidLogger.getLogger;
import static com.sshdaemon.util.ExternalStorage.createDirIfNotExists;
import static com.sshdaemon.util.ExternalStorage.getRootPath;
import static org.apache.sshd.common.compression.BuiltinCompressions.delayedZlib;
import static org.apache.sshd.common.compression.BuiltinCompressions.zlib;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.sshdaemon.MainActivity;
import com.sshdaemon.R;
import com.sshdaemon.util.AndroidLogger;

import org.apache.sshd.common.cipher.BuiltinCiphers;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.util.security.SecurityProviderChoice;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.common.util.threads.ThreadUtils;
import org.apache.sshd.contrib.server.subsystem.sftp.SimpleAccessControlSftpEventListener;
import org.apache.sshd.server.ServerBuilder;
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
import java.util.List;
import java.util.Map;

/***
 * __     _        ___
 * / _\___| |__    /   \__ _  ___ _ __ ___   ___  _ __
 * \ \/ __| '_ \  / /\ / _` |/ _ \ '_ ` _ \ / _ \| '_ \
 * _\ \__ \ | | |/ /_// (_| |  __/ | | | | | (_) | | | |
 * \__/___/_| |_/___,' \__,_|\___|_| |_| |_|\___/|_| |_|
 */


public class SshDaemon extends Service {

    public static final String AUTHORIZED_KEY_PATH = "SshDaemon/authorized_keys";
    public static final String CHANNEL_ID = "SshDaemonServiceChannel";
    public static final String SSH_DAEMON = "SshDaemon";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String SFTP_ROOT_PATH = "sftpRootPath";
    public static final String PASSWORD_AUTH_ENABLED = "passwordAuthenticationEnabled";
    public static final String READ_ONLY = "readOnly";
    private static final Logger logger = getLogger();
    private static final int THREAD_POOL_SIZE = 10;

    static {
        Security.removeProvider("BC");
        if (SecurityUtils.isRegistrationCompleted()) {
            logger.info("Security provider registration is already completed");
        } else {
            SecurityUtils.registerSecurityProvider(new ConsCryptSecurityProviderRegistrar());
            SecurityUtils.setDefaultProviderChoice(SecurityProviderChoice.toSecurityProviderChoice("ConsCryptWrapper"));
        }
        Security.addProvider(new BouncyCastleProvider());
    }

    private SshServer sshd;

    public SshDaemon() {
    }

    public SshDaemon(int port, String user, String password, String sftpRootPath, boolean passwordAuthenticationEnabled, boolean readOnly) {
        init(port, user, password, sftpRootPath, passwordAuthenticationEnabled, readOnly);
    }

    public static boolean publicKeyAuthenticationExists() {
        var authorizedKeyPath = getRootPath() + AUTHORIZED_KEY_PATH;
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
            var rootPath = getRootPath();
            var simpleGeneratorHostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get(rootPath + SSH_DAEMON + "/ssh_host_rsa_key"));
            var keyPairs = simpleGeneratorHostKeyProvider.loadKeys(null);
            final ECPublicKey publicKey = (ECPublicKey) keyPairs.get(0).getPublic();

            result.put(SshFingerprint.DIGESTS.MD5, fingerprintMD5(publicKey));
            result.put(SshFingerprint.DIGESTS.SHA256, fingerprintSHA256(publicKey));
        } catch (Exception e) {
            logger.error("Exception while getting fingerprints: ", e);
        }

        return result;
    }

    private void init(int port, String user, String password, String sftpRootPath, boolean passwordAuthenticationEnabled, boolean readOnly) {
        final var rootPath = getRootPath();
        final var path = rootPath + SSH_DAEMON;
        createDirIfNotExists(path);
        System.setProperty("user.home", sftpRootPath);

        this.sshd = ServerBuilder
                .builder()
                .cipherFactories(List.of(
                        BuiltinCiphers.aes128ctr,
                        BuiltinCiphers.aes192ctr,
                        BuiltinCiphers.aes256ctr,
                        BuiltinCiphers.aes128gcm,
                        BuiltinCiphers.aes256gcm))
                .compressionFactories(List.of(zlib, delayedZlib))
                .build();

        sshd.setPort(port);
        var authorizedKeyPath = rootPath + AUTHORIZED_KEY_PATH;
        var authorizedKeyFile = new File(authorizedKeyPath);
        if (authorizedKeyFile.exists()) {
            final SshPublicKeyAuthenticator sshPublicKeyAuthenticator = new SshPublicKeyAuthenticator();
            sshPublicKeyAuthenticator.loadKeysFromPath(authorizedKeyPath);
            sshd.setPublickeyAuthenticator(sshPublicKeyAuthenticator);
        }
        if (passwordAuthenticationEnabled || !authorizedKeyFile.exists()) {
            sshd.setPasswordAuthenticator(new SshPasswordAuthenticator(user, password));
        }

        var simpleGeneratorHostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get(path + "/ssh_host_rsa_key"));
        sshd.setKeyPairProvider(simpleGeneratorHostKeyProvider);
        sshd.setShellFactory(new InteractiveProcessShellFactory());
        var threadPools = max(THREAD_POOL_SIZE, Runtime.getRuntime().availableProcessors() * 2);
        logger.info("Thread pool size: {}", threadPools);

        var factory = new SftpSubsystemFactory.Builder()
                .withExecutorServiceProvider(() ->
                        ThreadUtils.newFixedThreadPool("SFTP-Subsystem", threadPools))
                .build();
        if (readOnly) {
            factory.addSftpEventListener((SimpleAccessControlSftpEventListener.READ_ONLY_ACCESSOR));
        }
        sshd.setSubsystemFactories(Collections.singletonList(factory));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(sftpRootPath)));

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
            var sftpRootPath = requireNonNull(intent.getStringExtra(SFTP_ROOT_PATH), "SFTP root path should be not null!");
            var passwordAuthenticationEnabled = intent.getBooleanExtra(PASSWORD_AUTH_ENABLED, true);
            var readOnly = intent.getBooleanExtra(READ_ONLY, false);
            init(port, user, password, sftpRootPath, passwordAuthenticationEnabled, readOnly);
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