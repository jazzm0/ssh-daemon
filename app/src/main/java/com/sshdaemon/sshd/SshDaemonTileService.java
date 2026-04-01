package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshDaemon.ACTION_SERVICE_STATE_CHANGED;
import static com.sshdaemon.sshd.SshDaemon.INTERFACE;
import static com.sshdaemon.sshd.SshDaemon.PASSWORD;
import static com.sshdaemon.sshd.SshDaemon.PASSWORD_AUTH_ENABLED;
import static com.sshdaemon.sshd.SshDaemon.PORT;
import static com.sshdaemon.sshd.SshDaemon.READ_ONLY;
import static com.sshdaemon.sshd.SshDaemon.SFTP_ROOT_PATH;
import static com.sshdaemon.sshd.SshDaemon.USER;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.core.content.ContextCompat;

import com.sshdaemon.MainActivity;
import com.sshdaemon.R;

public class SshDaemonTileService extends TileService {

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            syncTileState();
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        ContextCompat.registerReceiver(this, stateReceiver,
                new IntentFilter(ACTION_SERVICE_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        syncTileState();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unregisterReceiver(stateReceiver);
    }

    @Override
    public void onClick() {
        super.onClick();
        if (SshDaemon.isRunning()) {
            stopSshDaemon();
        } else {
            startSshDaemon();
        }
    }

    private void syncTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;
        if (SshDaemon.isRunning()) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel(getString(R.string.app_name));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.server_status_running));
            }
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.app_name));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.server_status_stopped));
            }
        }
        tile.updateTile();
    }

    private void startSshDaemon() {
        SharedPreferences prefs = getSharedPreferences("com.sshdaemon.MainActivity", MODE_PRIVATE);

        boolean passwordAuthEnabled = prefs.getBoolean(getString(R.string.password_authentication_enabled), true);
        String password = prefs.getString(getString(R.string.default_password_value), null);

        if (passwordAuthEnabled && (password == null || password.isEmpty()) && !SshDaemon.publicKeyAuthenticationExists()) {
            // Can't start without credentials — open the app instead
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(
                        this, 0, intent, PendingIntent.FLAG_IMMUTABLE));
            } else {
                //noinspection StartActivityAndCollapseDeprecated
                startActivityAndCollapse(intent);
            }
            return;
        }

        String selectedInterface = prefs.getString(getString(R.string.select_network_interface), null);
        String portStr = prefs.getString(getString(R.string.default_port_value), getString(R.string.default_port_value));
        String user = prefs.getString(getString(R.string.default_user_value), getString(R.string.default_user_value));
        String sftpRootPath = prefs.getString(getString(R.string.sftp_root_path), "/");
        boolean readOnly = prefs.getBoolean(getString(R.string.read_only), false);

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            port = 8022;
        }

        Intent intent = new Intent(this, SshDaemon.class);
        intent.putExtra(INTERFACE, selectedInterface);
        intent.putExtra(PORT, port);
        intent.putExtra(USER, user);
        intent.putExtra(PASSWORD, password != null ? password : "");
        intent.putExtra(SFTP_ROOT_PATH, sftpRootPath);
        intent.putExtra(PASSWORD_AUTH_ENABLED, passwordAuthEnabled);
        intent.putExtra(READ_ONLY, readOnly);

        ContextCompat.startForegroundService(this, intent);
    }

    private void stopSshDaemon() {
        stopService(new Intent(this, SshDaemon.class));
    }
}
