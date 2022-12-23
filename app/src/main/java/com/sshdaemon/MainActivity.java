package com.sshdaemon;

import static com.sshdaemon.sshd.SshDaemon.PASSWORD;
import static com.sshdaemon.sshd.SshDaemon.PORT;
import static com.sshdaemon.sshd.SshDaemon.SSH_DAEMON;
import static com.sshdaemon.sshd.SshDaemon.USER;
import static com.sshdaemon.sshd.SshDaemon.getFingerPrints;
import static com.sshdaemon.sshd.SshDaemon.publicKeyAuthenticationExists;
import static com.sshdaemon.sshd.SshPassword.getRandomString;
import static com.sshdaemon.util.TextViewHelper.createTextView;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sshdaemon.net.NetworkChangeReceiver;
import com.sshdaemon.sshd.SshDaemon;
import com.sshdaemon.sshd.SshFingerprint;

import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private String getValue(EditText t) {
        return t.getText().toString().equals("") ? t.getHint().toString() : t.getText().toString();
    }

    private void enableViews(boolean enable) {
        TextInputEditText port = findViewById(R.id.port_value);
        TextInputEditText user = findViewById(R.id.user_value);
        TextInputEditText password = findViewById(R.id.password_value);
        Button generate = findViewById(R.id.generate);
        port.setEnabled(enable);
        user.setEnabled(enable);
        password.setEnabled(enable);
        generate.setClickable(enable);

        View view = findViewById(R.id.start_stop_action);
        FloatingActionButton button = (FloatingActionButton) view;
        if (enable) {
            button.setImageResource(R.drawable.play_arrow_black_24dp);
        } else {
            button.setImageResource(R.drawable.pause_black_24dp);
        }
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

    private boolean isStarted() {
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        @SuppressWarnings("deprecation")
        List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(1);
        boolean started = false;
        if (!runningServices.isEmpty() && runningServices.get(0).service.flattenToString().contains(SSH_DAEMON)) {
            started = runningServices.get(0).started;
        }
        return started;
    }

    private void updateViews() {
        enableViews(!isStarted());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        LinearLayout linearLayout = findViewById(R.id.network_interfaces);

        this.registerReceiver(new NetworkChangeReceiver(linearLayout, this),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        } else if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !getSystemService(NotificationManager.class).areNotificationsEnabled()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        setFingerPrints(getFingerPrints());

        ImageView imageView = findViewById(R.id.key_based_authentication);

        if (publicKeyAuthenticationExists()) {
            imageView.setImageResource(R.drawable.key_black_24dp);
        } else {
            imageView.setImageResource(R.drawable.key_off_black_24dp);
        }
        updateViews();
    }

    public void keyClicked(View view) {
        CharSequence text = publicKeyAuthenticationExists() ?
                getResources().getString(R.string.ssh_public_key_exists) :
                getResources().getString(R.string.ssh_public_key_doesnt_exists);

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void generateClicked(View view) {
        TextInputEditText password = findViewById(R.id.password_value);
        password.setText(getRandomString(5));
    }

    public void startStopClicked(View view) {

        if (isStarted()) {
            enableViews(true);
            stopService();
        } else {
            enableViews(false);
            setFingerPrints(getFingerPrints());
            final String port = getValue(findViewById(R.id.port_value));
            final String user = getValue(findViewById(R.id.user_value));
            final String password = getValue(findViewById(R.id.password_value));

            startService(Integer.parseInt(port), user, password);
        }
    }

    public void startService(int port, String user, String password) {
        Intent sshDaemonIntent = new Intent(this, SshDaemon.class);
        sshDaemonIntent.putExtra(PORT, port);
        sshDaemonIntent.putExtra(USER, user);
        sshDaemonIntent.putExtra(PASSWORD, password);
        ContextCompat.startForegroundService(this, sshDaemonIntent);
    }

    public void stopService() {
        Intent sshDaemonIntent = new Intent(this, SshDaemon.class);
        stopService(sshDaemonIntent);
    }
}
