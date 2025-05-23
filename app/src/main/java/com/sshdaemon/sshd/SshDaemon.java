package com.sshdaemon.sshd;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintMD5;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintSHA256;
import static com.sshdaemon.util.AndroidLogger.getLogger;
import static com.sshdaemon.util.ExternalStorage.createDirIfNotExists;
import static com.sshdaemon.util.ExternalStorage.getRootPath;
import static org.apache.sshd.common.cipher.BuiltinCiphers.aes128ctr;
import static org.apache.sshd.common.cipher.BuiltinCiphers.aes128gcm;
import static org.apache.sshd.common.cipher.BuiltinCiphers.aes192ctr;
import static org.apache.sshd.common.cipher.BuiltinCiphers.aes256ctr;
import static org.apache.sshd.common.cipher.BuiltinCiphers.aes256gcm;
import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import android.app.Notification;
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

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
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

public class SshDaemon extends Service {

    public static final int NOTIFICATION_ID = 1;
    public static final String AUTHORIZED_KEY_PATH = "SshDaemon/authorized_keys";
    public static final String CHANNEL_ID = "SshDaemonServiceChannel";
    public static final String SSH_DAEMON = "SshDaemon";
    public static final String INTERFACE = "interface";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String SFTP_ROOT_PATH = "sftpRootPath";
    public static final String PASSWORD_AUTH_ENABLED = "passwordAuthenticationEnabled";
    public static final String READ_ONLY = "readOnly";
    private static final Logger logger = getLogger();
    private static final int THREAD_POOL_SIZE = 10;
    private static final int DEFAULT_PORT = 8022;

    static {
        Security.removeProvider("BC");
        if (SecurityUtils.isRegistrationCompleted()) {
            logger.info("Security provider registration is already completed");
        } else {
            try {
                Security.addProvider(new BouncyCastleProvider());
                logger.info("Set security provider to:{}, registration completed:{}", BouncyCastleProvider.PROVIDER_NAME, SecurityUtils.isRegistrationCompleted());
            } catch (Exception e) {
                logger.error("Exception while registering security provider: ", e);
            }
        }
    }

    private SshServer sshd;

    public SshDaemon() {
        // Default constructor required for Service
    }

    public SshDaemon(String selectedInterface, int port, String user, String password, String sftpRootPath,
                     boolean passwordAuthEnabled, boolean readOnly) {
        init(selectedInterface, port, user, password, sftpRootPath, passwordAuthEnabled, readOnly);
    }

    public static boolean publicKeyAuthenticationExists() {
        var authorizedKeyPath = getRootPath() + AUTHORIZED_KEY_PATH;
        var authorizedKeyFile = new File(authorizedKeyPath);
        if (!authorizedKeyFile.exists()) {
            return false;
        }
        var authenticator = new SshPublicKeyAuthenticator();
        return authenticator.loadKeysFromPath(authorizedKeyPath);
    }

    public static Map<SshFingerprint.DIGESTS, String> getFingerPrints() {
        var result = new HashMap<SshFingerprint.DIGESTS, String>();
        try {
            var rootPath = getRootPath();
            var keyProvider =
                    new SimpleGeneratorHostKeyProvider(Paths.get(rootPath + SSH_DAEMON + "/ssh_host_rsa_key"));
            var keyPairs = keyProvider.loadKeys(null);
            if (!keyPairs.isEmpty()) {
                ECPublicKey publicKey = (ECPublicKey) keyPairs.get(0).getPublic();
                result.put(SshFingerprint.DIGESTS.MD5, fingerprintMD5(publicKey));
                result.put(SshFingerprint.DIGESTS.SHA256, fingerprintSHA256(publicKey));
            } else {
                logger.warn("No host key pairs available");
            }
        } catch (Exception e) {
            logger.error("Failed to get fingerprints", e);
        }
        return result;
    }

    private void init(String selectedInterface, int port, String user, String password, String sftpRootPath,
                      boolean passwordAuthEnabled, boolean readOnly) {

        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1024 and 65535");
        }
        var sftpRoot = new File(sftpRootPath);
        if (!sftpRoot.exists() || !sftpRoot.canWrite()) {
            throw new IllegalArgumentException("SFTP root path does not exist or is not writable");
        }

        var rootPath = getRootPath();
        var path = rootPath + SSH_DAEMON;
        createDirIfNotExists(path);
        System.setProperty("user.home", sftpRootPath);

        this.sshd = ServerBuilder
                .builder()
                .cipherFactories(List.of(aes128ctr, aes192ctr, aes256ctr, aes128gcm, aes256gcm))
                .build();

        if (!isNull(selectedInterface)) {
            sshd.setHost(selectedInterface);
        }

        sshd.setPort(port);

        var authorizedKeyPath = rootPath + AUTHORIZED_KEY_PATH;
        var authorizedKeyFile = new File(authorizedKeyPath);
        if (authorizedKeyFile.exists()) {
            final var authenticator = new SshPublicKeyAuthenticator();
            if (authenticator.loadKeysFromPath(authorizedKeyPath)) {
                sshd.setPublickeyAuthenticator(authenticator);
            } else {
                logger.warn("Failed to load authorized keys from {}", authorizedKeyPath);
            }
        }

        if (passwordAuthEnabled || !authorizedKeyFile.exists()) {
            sshd.setPasswordAuthenticator(new SshPasswordAuthenticator(user, password));
        }

        var keyProvider =
                new SimpleGeneratorHostKeyProvider(Paths.get(path + "/ssh_host_rsa_key"));
        sshd.setKeyPairProvider(keyProvider);
        sshd.setShellFactory(new InteractiveProcessShellFactory());

        int threadPools = max(THREAD_POOL_SIZE, Runtime.getRuntime().availableProcessors() * 2);
        logger.info("Thread pool size: {}", threadPools);
        SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder()
                .withExecutorServiceProvider(() ->
                        ThreadUtils.newFixedThreadPool("SFTP-Subsystem", threadPools))
                .build();
        if (readOnly) {
            factory.addSftpEventListener(SimpleAccessControlSftpEventListener.READ_ONLY_ACCESSOR);
        }
        sshd.setSubsystemFactories(Collections.singletonList(factory));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(sftpRootPath)));
    }

    private Notification createNotification(String contentText, PendingIntent pendingIntent) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(SSH_DAEMON)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.play_arrow_fill0_wght400_grad0_opsz48)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        var notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        var pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            var serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_HIGH
            );

            var manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);

            var notification = createNotification(SSH_DAEMON, pendingIntent);
            startForeground(NOTIFICATION_ID, notification);

            var interfaceName = intent.getStringExtra(INTERFACE);
            var port = intent.getIntExtra(PORT, DEFAULT_PORT);
            var user = requireNonNull(intent.getStringExtra(USER), "User must not be null");
            var password = requireNonNull(intent.getStringExtra(PASSWORD), "Password must not be null");
            var sftpRootPath = requireNonNull(intent.getStringExtra(SFTP_ROOT_PATH),
                    "SFTP root path must not be null");
            var passwordAuthEnabled = intent.getBooleanExtra(PASSWORD_AUTH_ENABLED, true);
            var readOnly = intent.getBooleanExtra(READ_ONLY, false);

            init(interfaceName, port, user, password, sftpRootPath, passwordAuthEnabled, readOnly);
            sshd.start();
            logger.info("SSH daemon started on port {}", port);
            updateNotification("SSH Server Running on port " + port, pendingIntent);
        } catch (IOException e) {
            logger.error("Failed to start SSH daemon", e);
            updateNotification("Failed to start SSH Server: " + e.getMessage(), pendingIntent);
            stopSelf();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid configuration", e);
            updateNotification("Invalid configuration: " + e.getMessage(), pendingIntent);
            stopSelf();
        }
        return START_STICKY;
    }

    private void updateNotification(String status, PendingIntent pendingIntent) {
        Notification notification = createNotification(status, pendingIntent);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (sshd != null && sshd.isStarted()) {
                sshd.stop();
                logger.info("SSH daemon stopped");
                var notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
                var pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        0, notificationIntent, FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                updateNotification("SSH Server Stopped", pendingIntent);
                stopForeground(true);
            }
        } catch (IOException e) {
            logger.error("Failed to stop SSH daemon", e);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}