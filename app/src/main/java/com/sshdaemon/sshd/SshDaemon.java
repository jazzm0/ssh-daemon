package com.sshdaemon.sshd;

import static android.app.PendingIntent.FLAG_MUTABLE;
import static com.sshdaemon.util.ExternalStorage.createDirIfNotExists;

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

/***
 * __     _        ___
 * / _\___| |__    /   \__ _  ___ _ __ ___   ___  _ __
 * \ \/ __| '_ \  / /\ / _` |/ _ \ '_ ` _ \ / _ \| '_ \
 * _\ \__ \ | | |/ /_// (_| |  __/ | | | | | (_) | | | |
 * \__/___/_| |_/___,' \__,_|\___|_| |_| |_|\___/|_| |_|
 */


public class SshDaemon extends Service {

    public static String SSH_DAEMON = "SshDaemon";
    public static String PATH = "path";
    public static String PORT = "port";
    public static String USER = "user";
    public static String PASSWORD = "PASSWORD";

    public static final String AUTHORIZED_KEY_PATH = "/SshDaemon/authorized_keys";
    public static final String CHANNEL_ID = "SshDaemonServiceChannel";
    private SshServer sshd;
    private List<KeyPair> keyPairs;

    public SshDaemon() {
    }

    public SshDaemon(String rootPath, int port, String user, String password) {
        init(rootPath, port, user, password);
    }

    private void init(String rootPath, int port, String user, String password) {
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
        this.keyPairs = simpleGeneratorHostKeyProvider.loadKeys(null);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
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

    public Map<SshFingerprint.DIGESTS, String> getFingerPrints() throws NoSuchAlgorithmException {
        final Map<SshFingerprint.DIGESTS, String> result = new HashMap<>();
        final RSAPublicKey publicKey = (RSAPublicKey) this.keyPairs.get(0).getPublic();

        result.put(SshFingerprint.DIGESTS.MD5, SshFingerprint.fingerprintMD5(publicKey.getPublicExponent(), publicKey.getModulus()));
        result.put(SshFingerprint.DIGESTS.SHA256, SshFingerprint.fingerprintSHA256(publicKey.getPublicExponent(), publicKey.getModulus()));

        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String rootPath = intent.getStringExtra(PATH);
            int port = intent.getIntExtra(PORT, 8022);
            String user = intent.getStringExtra(USER);
            String password = intent.getStringExtra(PASSWORD);

            init(rootPath, port, user, password);

            createNotificationChannel();

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, FLAG_MUTABLE);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(SSH_DAEMON)
                    .setContentText("input")
                    .setSmallIcon(R.drawable.play_arrow_fill0_wght400_grad0_opsz48)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);
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