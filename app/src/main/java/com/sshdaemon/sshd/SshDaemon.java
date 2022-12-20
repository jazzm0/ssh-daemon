package com.sshdaemon.sshd;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.sshdaemon.sshd.SshFingerprint.encode;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintMD5;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintSHA256;
import static com.sshdaemon.util.ExternalStorage.createDirIfNotExists;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import android.app.Notification;
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
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
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

    public static final String AUTHORIZED_KEY_PATH = "/SshDaemon/authorized_keys";
    public static final String CHANNEL_ID = "SshDaemonServiceChannel";
    public static final String SSH_DAEMON = "SshDaemon";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "PASSWORD";
    private SshServer sshd;

    public SshDaemon() {
    }

    public SshDaemon(int port, String user, String password) {
        init(port, user, password);
    }

    public static boolean publicKeyAuthenticationExists() {
        String authorizedKeyPath = Environment.getExternalStorageDirectory().getPath() + AUTHORIZED_KEY_PATH;
        File authorizedKeyFile = new File(authorizedKeyPath);
        boolean authorizedKeysExist = false;
        if (authorizedKeyFile.exists()) {
            final SshPublicKeyAuthenticator sshPublicKeyAuthenticator = new SshPublicKeyAuthenticator();
            authorizedKeysExist = sshPublicKeyAuthenticator.loadKeysFromPath(authorizedKeyPath);
        }
        return authorizedKeysExist;
    }

    public static Map<SshFingerprint.DIGESTS, String> getFingerPrints() throws NoSuchAlgorithmException {
        String rootPath = isNull(Environment.getExternalStorageDirectory()) ? "/" : Environment.getExternalStorageDirectory().getPath();
        SimpleGeneratorHostKeyProvider simpleGeneratorHostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get(rootPath + "/" + SSH_DAEMON + "/ssh_host_rsa_key"));
        List<KeyPair> keyPairs = simpleGeneratorHostKeyProvider.loadKeys(null);
        final Map<SshFingerprint.DIGESTS, String> result = new HashMap<>();
        final ECPublicKey publicKey = (ECPublicKey) keyPairs.get(0).getPublic();
        final byte[] encodedKey = encode(publicKey);
        result.put(SshFingerprint.DIGESTS.MD5, fingerprintMD5(encodedKey));
        result.put(SshFingerprint.DIGESTS.SHA256, fingerprintSHA256(encodedKey));

        return result;
    }

    private void init(int port, String user, String password) {
        final String rootPath = Environment.getExternalStorageDirectory().getPath();
        final String path = rootPath + "/" + SSH_DAEMON;
        createDirIfNotExists(path);
        System.setProperty("user.home", rootPath);
        this.sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setPasswordAuthenticator(new SshPasswordAuthenticator(user, password));
        String authorizedKeyPath = rootPath + AUTHORIZED_KEY_PATH;
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
        simpleGeneratorHostKeyProvider.loadKeys(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);

            Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    0, notificationIntent, FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setContentTitle(SSH_DAEMON)
                    .setContentText(SSH_DAEMON)
                    .setSmallIcon(R.drawable.play_arrow_fill0_wght400_grad0_opsz48)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);

            int port = intent.getIntExtra(PORT, 8022);
            String user = requireNonNull(intent.getStringExtra(USER), "User should be not null!");
            String password = requireNonNull(intent.getStringExtra(PASSWORD), "Password should be not null!");
            init(port, user, password);
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