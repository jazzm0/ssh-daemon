package com.sshdaemon;

import static android.text.TextUtils.TruncateAt.END;
import static com.sshdaemon.sshd.SshDaemon.INTERFACE;
import static com.sshdaemon.sshd.SshDaemon.NOTIFICATION_ID;
import static com.sshdaemon.sshd.SshDaemon.PASSWORD;
import static com.sshdaemon.sshd.SshDaemon.PASSWORD_AUTH_ENABLED;
import static com.sshdaemon.sshd.SshDaemon.PORT;
import static com.sshdaemon.sshd.SshDaemon.READ_ONLY;
import static com.sshdaemon.sshd.SshDaemon.SFTP_ROOT_PATH;
import static com.sshdaemon.sshd.SshDaemon.SSH_DAEMON;
import static com.sshdaemon.sshd.SshDaemon.USER;
import static com.sshdaemon.sshd.SshDaemon.getFingerPrints;
import static com.sshdaemon.sshd.SshDaemon.publicKeyAuthenticationExists;
import static com.sshdaemon.sshd.SshPassword.getRandomString;
import static com.sshdaemon.util.ExternalStorage.getAllStorageLocations;
import static com.sshdaemon.util.ExternalStorage.hasMultipleStorageLocations;
import static com.sshdaemon.util.TextViewHelper.createTextView;
import static java.util.Objects.isNull;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.sshdaemon.net.NetworkChangeReceiver;
import com.sshdaemon.sshd.SshDaemon;
import com.sshdaemon.sshd.SshFingerprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private String selectedInterface;

    private String getValue(EditText t) {
        return t.getText().toString().isEmpty() ? t.getHint().toString() : t.getText().toString();
    }

    private void createSpinnerAdapter(Spinner sftpRootPaths) {
        if (isNull(sftpRootPaths.getSelectedItem())) {
            var adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, getAllStorageLocations(this));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sftpRootPaths.setAdapter(adapter);
        }
    }

    private void enableViews(boolean enable) {
        var selectedInterface = findViewById(R.id.network_interface_spinner);
        var port = findViewById(R.id.port_value);
        var user = findViewById(R.id.user_value);
        var password = findViewById(R.id.password_value);
        var sftpRootPaths = (Spinner) findViewById(R.id.sftp_paths);
        var generate = findViewById(R.id.generate);
        var passwordAuthenticationEnabled = (SwitchMaterial) findViewById(R.id.password_authentication_enabled);
        var readonly = findViewById(R.id.readonly_switch);
        var imageView = (ImageView) findViewById(R.id.key_based_authentication);

        selectedInterface.setEnabled(enable);
        port.setEnabled(enable);
        user.setEnabled(enable);
        password.setEnabled(enable);
        sftpRootPaths.setEnabled(enable);
        generate.setClickable(enable);
        readonly.setEnabled(enable);

        if (hasMultipleStorageLocations(this)) {
            sftpRootPaths.setVisibility(View.VISIBLE);
        } else {
            sftpRootPaths.setVisibility(View.GONE);
        }

        createSpinnerAdapter(sftpRootPaths);

        if (publicKeyAuthenticationExists()) {
            imageView.setImageResource(R.drawable.key_black_24dp);
            if (passwordAuthenticationEnabled.isChecked()) {
                setPasswordGroupVisibility(View.VISIBLE);
                enablePasswordAuthentication(enable, true);
            } else {
                setPasswordGroupVisibility(View.GONE);
                enablePasswordAuthentication(enable, false);
            }
        } else {
            imageView.setImageResource(R.drawable.key_off_black_24dp);
            setPasswordGroupVisibility(View.VISIBLE);
            enablePasswordAuthentication(enable && publicKeyAuthenticationExists(), true);
        }

        var view = findViewById(R.id.start_stop_action);
        var button = (FloatingActionButton) view;
        if (enable) {
            button.setImageResource(R.drawable.play_arrow_black_24dp);
        } else {
            button.setImageResource(R.drawable.pause_black_24dp);
        }
        updateServerStatusDisplay(enable);
    }

    private void setPasswordGroupVisibility(int visibility) {
        var generate = findViewById(R.id.generate);
        var passwordLayout = findViewById(R.id.password_layout);
        var userLayout = findViewById(R.id.user_layout);
        userLayout.setVisibility(visibility);
        passwordLayout.setVisibility(visibility);
        generate.setVisibility(visibility);
    }

    private void enablePasswordAuthentication(boolean enabled, boolean activated) {
        var passwordAuthenticationEnabled = (SwitchMaterial) findViewById(R.id.password_authentication_enabled);
        passwordAuthenticationEnabled.setEnabled(enabled);
        passwordAuthenticationEnabled.setChecked(activated);
        passwordAuthenticationEnabled.setActivated(activated);
    }

    private void setFingerPrints(Map<SshFingerprint.DIGESTS, String> fingerPrints) {

        LinearLayout fingerPrintsLayout = findViewById(R.id.server_fingerprints);

        fingerPrintsLayout.removeAllViews();

        var interfacesText = new TextView(this);
        interfacesText.setEllipsize(END);
        interfacesText.setSingleLine();
        interfacesText.setMaxLines(1);
        interfacesText.setTextSize(11);
        interfacesText.setText(R.string.fingerprints_label_text);
        interfacesText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        interfacesText.setTypeface(null, Typeface.BOLD);

        fingerPrintsLayout.addView(interfacesText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        for (Map.Entry<SshFingerprint.DIGESTS, String> e : fingerPrints.entrySet()) {
            var textView = createTextView(this, "(" + e.getKey() + ") " + e.getValue());
            fingerPrintsLayout.addView(textView,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }

    private boolean isStarted() {
        var am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        @SuppressWarnings("deprecation")
        var runningServices = am.getRunningServices(1);
        var started = false;
        if (!runningServices.isEmpty() && runningServices.get(0).service.flattenToString().contains(SSH_DAEMON)) {
            started = runningServices.get(0).started;
        }
        return started;
    }

    private void updateServerStatusDisplay(boolean enable) {
        androidx.cardview.widget.CardView statusCard = findViewById(R.id.server_status_card);
        TextView statusSummary = findViewById(R.id.server_status_summary);

        if (!enable) {
            String statusText = getServerStatusText();
            statusSummary.setText(statusText);
            statusCard.setVisibility(View.VISIBLE);
        } else {
            statusCard.setVisibility(View.GONE);
        }
    }

    private String getServerStatusText() {
        if (selectedInterface == null) {
            // "All interfaces" is selected
            var activeInterfaces = getActiveNetworkInterfaces();
            if (activeInterfaces.isEmpty()) {
                return "Server active on all available interfaces";
            } else {
                int count = activeInterfaces.size();
                String interfaceList = String.join(", ", activeInterfaces);
                return "Server active on: " + interfaceList + " (" + count + " interface" + (count > 1 ? "s" : "") + ")";
            }
        } else {
            // Specific interface is selected
            return "Server active on: " + selectedInterface;
        }
    }

    private List<String> getActiveNetworkInterfaces() {
        // Get the current list of interfaces from NetworkChangeReceiver
        // Since we can't directly access it, we'll get it from the Spinner adapter
        var networkInterfaceSpinner = (Spinner) findViewById(R.id.network_interface_spinner);
        var adapter = (ArrayAdapter<String>) networkInterfaceSpinner.getAdapter();

        List<String> activeInterfaces = new ArrayList<>();
        if (adapter != null && adapter.getCount() > 1) {
            // Skip the first item which is "all interfaces"
            for (int i = 1; i < adapter.getCount(); i++) {
                activeInterfaces.add(adapter.getItem(i));
            }
        }
        return activeInterfaces;
    }

    private void storeValues(String selectedInterface, String port, String user, boolean passwordAuthenticationEnabled, boolean readOnly, String sftpRootPath) {
        var editor = this.getPreferences(Context.MODE_PRIVATE).edit();

        editor.putString(getString(R.string.select_network_interface), selectedInterface);
        editor.putString(getString(R.string.default_port_value), port);
        editor.putString(getString(R.string.default_user_value), user);
        editor.putString(getString(R.string.sftp_root_path), sftpRootPath);
        editor.putBoolean(getString(R.string.password_authentication_enabled), passwordAuthenticationEnabled);
        editor.putBoolean(getString(R.string.read_only), readOnly);

        editor.apply();
    }

    private void restoreValues() {
        var preferences = this.getPreferences(Context.MODE_PRIVATE);
        var networkInterfaceSpinner = (Spinner) findViewById(R.id.network_interface_spinner);
        var port = (TextView) findViewById(R.id.port_value);
        var user = (TextView) findViewById(R.id.user_value);
        var passwordAuthenticationEnabled = (SwitchMaterial) findViewById(R.id.password_authentication_enabled);
        var readonly = (SwitchMaterial) findViewById(R.id.readonly_switch);
        var sftpRootPath = (Spinner) findViewById(R.id.sftp_paths);

        this.setSelectedInterface(preferences.getString(getString(R.string.select_network_interface), null));
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) networkInterfaceSpinner.getAdapter();

        var position = adapter.getPosition(this.selectedInterface);

        if (position >= 0) {
            networkInterfaceSpinner.setSelection(position);
        }

        port.setText(preferences.getString(getString(R.string.default_port_value), getString(R.string.default_port_value)));
        user.setText(preferences.getString(getString(R.string.default_user_value), getString(R.string.default_user_value)));
        passwordAuthenticationEnabled.setChecked(preferences.getBoolean(getString(R.string.password_authentication_enabled), true));
        readonly.setChecked(preferences.getBoolean(getString(R.string.read_only), false));
        createSpinnerAdapter(sftpRootPath);
        position = ((ArrayAdapter<String>) sftpRootPath.getAdapter()).getPosition(preferences.getString(getString(R.string.sftp_root_path), "/"));
        sftpRootPath.setSelection(position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (!isNull(notificationManager)) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreValues();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        var networkInterfaceSpinner = (Spinner) findViewById(R.id.network_interface_spinner);

        var connectivityManager = getSystemService(ConnectivityManager.class);
        connectivityManager.registerDefaultNetworkCallback(new NetworkChangeReceiver(networkInterfaceSpinner,
                this.getSystemService(ConnectivityManager.class),
                this));

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

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (view, insets) -> {
            var systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom);
            return insets;
        });

        setFingerPrints(getFingerPrints());
        generateClicked(null);
        restoreValues();
    }

    public void setSelectedInterface(String selectedInterface) {
        this.selectedInterface = selectedInterface;
    }

    public void keyClicked(View view) {
        var text = publicKeyAuthenticationExists() ?
                getResources().getString(R.string.ssh_public_key_exists) :
                getResources().getString(R.string.ssh_public_key_doesnt_exists);

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void generateClicked(View view) {
        TextInputEditText password = findViewById(R.id.password_value);
        password.setText(getRandomString(6));
    }

    public void passwordSwitchClicked(View passwordAuthenticationEnabled) {
        var passwordSwitch = (SwitchMaterial) passwordAuthenticationEnabled;
        enablePasswordAuthentication(true, !passwordSwitch.isActivated());
    }

    public void startStopClicked(View view) {
        if (isStarted()) {
            enableViews(true);
            stopService();
        } else {
            enableViews(false);
            setFingerPrints(getFingerPrints());
            final var port = getValue(findViewById(R.id.port_value));
            final var user = getValue(findViewById(R.id.user_value));
            final var password = getValue(findViewById(R.id.password_value));
            final var sftpRootPath = ((Spinner) findViewById(R.id.sftp_paths)).getSelectedItem().toString();
            final var passwordAuthenticationEnabled = ((SwitchMaterial) findViewById(R.id.password_authentication_enabled)).isChecked();
            final var readOnly = ((SwitchMaterial) findViewById(R.id.readonly_switch)).isChecked();
            storeValues(this.selectedInterface, port, user, passwordAuthenticationEnabled, readOnly, sftpRootPath);

            startService(Integer.parseInt(port), user, password, sftpRootPath, passwordAuthenticationEnabled, readOnly);
        }
    }

    public void startService(int port, String user, String password, String sftpRootPath, boolean passwordAuthenticationEnabled, boolean readOnly) {
        var sshDaemonIntent = new Intent(this, SshDaemon.class);
        sshDaemonIntent.putExtra(INTERFACE, selectedInterface);
        sshDaemonIntent.putExtra(PORT, port);
        sshDaemonIntent.putExtra(USER, user);
        sshDaemonIntent.putExtra(PASSWORD, password);
        sshDaemonIntent.putExtra(SFTP_ROOT_PATH, sftpRootPath);
        sshDaemonIntent.putExtra(PASSWORD_AUTH_ENABLED, passwordAuthenticationEnabled);
        sshDaemonIntent.putExtra(READ_ONLY, readOnly);

        ContextCompat.startForegroundService(this, sshDaemonIntent);
    }

    public void stopService() {
        var sshDaemonIntent = new Intent(this, SshDaemon.class);
        stopService(sshDaemonIntent);
    }
}
