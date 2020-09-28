package com.sshdaemon;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sshdaemon.net.NetworkChangeReceiver;
import com.sshdaemon.sshd.SshDaemon;
import com.sshdaemon.sshd.SshFingerprint;

import java.util.Map;
import java.util.Objects;

import static com.sshdaemon.sshd.SshPassword.getRandomString;
import static com.sshdaemon.util.TextViewHelper.createTextView;


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
        Button generate = findViewById(R.id.generate);
        port.setEnabled(enable);
        user.setEnabled(enable);
        password.setEnabled(enable);
        generate.setClickable(enable);
    }

    private void releaseWakeLock() {
        if (wakeLock.isHeld()) wakeLock.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void acquireWakelock() {
        if (!wakeLock.isHeld()) wakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setFingerPrints(Map<SshFingerprint.DIGESTS, String> fingerPrints) {

        LinearLayout fingerPrintsLayout = findViewById(R.id.server_fingerprints);

        fingerPrintsLayout.removeAllViews();

        TextView interfacesText = new TextView(this);
        interfacesText.setText(R.string.fingerprints_label_text);
        interfacesText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        interfacesText.setTypeface(null, Typeface.BOLD);

        fingerPrintsLayout.addView(interfacesText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        for (Map.Entry<SshFingerprint.DIGESTS, String> e : fingerPrints.entrySet()) {
            TextView textView = createTextView(this, "(" + e.getKey() + ") " + e.getValue());
            fingerPrintsLayout.addView(textView,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        LinearLayout linearLayout = findViewById(R.id.network_interfaces);

        this.registerReceiver(new NetworkChangeReceiver(linearLayout, this),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP),
                "SshDaemon:SshDaemonWakeLock");

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

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

    public void generateClicked(View view) {
        TextInputEditText password = findViewById(R.id.password_value);
        password.setText(getRandomString(5));
    }

    public void startStopClicked(View view) {
        TextInputEditText port = findViewById(R.id.port_value);
        TextInputEditText user = findViewById(R.id.user_value);
        TextInputEditText password = findViewById(R.id.password_value);
//        String path = Objects.requireNonNull(getExternalFilesDir(null)).toString();
        String path = Environment.getExternalStorageDirectory().getPath();

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
                setFingerPrints(sshDaemon.getFingerPrints());
                sshDaemon.start();
                enableInput(false);
                button.setImageResource(R.drawable.pause);
            }
        } catch (Exception e) {
        }
    }
}
