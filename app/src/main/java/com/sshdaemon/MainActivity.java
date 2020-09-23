package com.sshdaemon;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sshdaemon.net.NetworkChangeReceiver;
import com.sshdaemon.sshd.SshDaemon;

import java.io.IOException;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private SshDaemon sshDaemon;
    private PowerManager.WakeLock wakeLock;

    private String getValue(EditText t) {
        return t.getText().toString().equals("") ? t.getHint().toString() : t.getText().toString();
    }

    private void enableInput(boolean enable) {
        TextInputEditText port = findViewById(R.id.port_value);
        TextInputEditText user = findViewById(R.id.user_value);
        TextInputEditText password = findViewById(R.id.password_value);
        port.setEnabled(enable);
        user.setEnabled(enable);
        password.setEnabled(enable);
    }

    private void releaseWakeLock() {
        if (wakeLock.isHeld()) wakeLock.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void acquireWakelock() {
        if (!wakeLock.isHeld()) wakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        LinearLayout linearLayout = findViewById(R.id.network_interfaces);

        this.registerReceiver(new NetworkChangeReceiver(linearLayout, this),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP),
                "SshDaemon:SshDaemonWakeLock");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sshDaemon != null)
            startStopClicked(findViewById(R.id.start_stop_action));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sshDaemon != null)
            startStopClicked(findViewById(R.id.start_stop_action));
    }

    public void startStopClicked(View view) {
        TextInputEditText port = findViewById(R.id.port_value);
        TextInputEditText user = findViewById(R.id.user_value);
        TextInputEditText password = findViewById(R.id.password_value);
        String path = Objects.requireNonNull(getExternalFilesDir(null)).toString();

        String realPort = getValue(port);
        if (realPort.equals("Port")) realPort = "8022";
        String realUser = getValue(user);
        String realPassword = getValue(password);

        FloatingActionButton button = (FloatingActionButton) view;

        try {
            if (!Objects.isNull(sshDaemon) && sshDaemon.isRunning()) {
                releaseWakeLock();
                sshDaemon.stop();
                enableInput(true);
                button.setImageResource(R.drawable.play);
            } else {
                acquireWakelock();
                sshDaemon = new SshDaemon(path, Integer.parseInt(realPort), realUser, realPassword);
                sshDaemon.start();
                enableInput(false);
                button.setImageResource(R.drawable.pause);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
