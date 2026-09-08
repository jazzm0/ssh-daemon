package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshDaemon.ACTION_SERVICE_STATE_CHANGED;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
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
            tile.setIcon(Icon.createWithResource(this, R.drawable.pause_black_24dp));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.server_status_running));
            }
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.app_name));
            tile.setIcon(Icon.createWithResource(this, R.drawable.play_arrow_black_24dp));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.server_status_stopped));
            }
        }
        tile.updateTile();
    }

    // startActivityAndCollapse(Intent) is deprecated in API 34+; the PendingIntent
    // overload is used there, and the Intent fallback is only reached on API 26-33.
    @SuppressWarnings("deprecation")
    private void startSshDaemon() {
        SharedPreferences prefs = getSharedPreferences(SshDaemon.PREFS_NAME, MODE_PRIVATE);

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

        // The service reads its configuration from preferences; the intent is only a
        // start trigger.
        ContextCompat.startForegroundService(this, new Intent(this, SshDaemon.class));
    }

    private void stopSshDaemon() {
        stopService(new Intent(this, SshDaemon.class));
    }
}
