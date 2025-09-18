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
    private ViewHolder views;

    // ViewHolder pattern to cache view references
    private static class ViewHolder {
        final Spinner networkInterfaceSpinner;
        final EditText portValue;
        final EditText userValue;
        final EditText passwordValue;
        final Spinner sftpPaths;
        final View generate;
        final SwitchMaterial passwordAuthenticationEnabled;
        final SwitchMaterial readonly;
        final ImageView keyBasedAuthentication;
        final FloatingActionButton startStopAction;
        final androidx.cardview.widget.CardView serverStatusCard;
        final TextView serverStatusSummary;
        final LinearLayout serverFingerprints;
        final View passwordLayout;
        final View userLayout;

        ViewHolder(MainActivity activity) {
            networkInterfaceSpinner = activity.findViewById(R.id.network_interface_spinner);
            portValue = activity.findViewById(R.id.port_value);
            userValue = activity.findViewById(R.id.user_value);
            passwordValue = activity.findViewById(R.id.password_value);
            sftpPaths = activity.findViewById(R.id.sftp_paths);
            generate = activity.findViewById(R.id.generate);
            passwordAuthenticationEnabled = activity.findViewById(R.id.password_authentication_enabled);
            readonly = activity.findViewById(R.id.readonly_switch);
            keyBasedAuthentication = activity.findViewById(R.id.key_based_authentication);
            startStopAction = activity.findViewById(R.id.start_stop_action);
            serverStatusCard = activity.findViewById(R.id.server_status_card);
            serverStatusSummary = activity.findViewById(R.id.server_status_summary);
            serverFingerprints = activity.findViewById(R.id.server_fingerprints);
            passwordLayout = activity.findViewById(R.id.password_layout);
            userLayout = activity.findViewById(R.id.user_layout);
        }
    }

    // UI State Management
    private void enableViews(boolean enable) {
        setupBasicViewStates(enable);
        configureSpinnerVisibility();
        configureAuthenticationViews(enable);
        updateActionButton(enable);
        updateServerStatusDisplay(enable);
    }

    private void setupBasicViewStates(boolean enable) {
        views.networkInterfaceSpinner.setEnabled(enable);
        views.portValue.setEnabled(enable);
        views.userValue.setEnabled(enable);
        views.passwordValue.setEnabled(enable);
        views.sftpPaths.setEnabled(enable);
        views.generate.setClickable(enable);
        views.readonly.setEnabled(enable);
    }

    private void configureSpinnerVisibility() {
        int visibility = hasMultipleStorageLocations(this) ? View.VISIBLE : View.GONE;
        views.sftpPaths.setVisibility(visibility);
        createSpinnerAdapter(views.sftpPaths);
    }

    private void configureAuthenticationViews(boolean enable) {
        boolean hasPublicKey = publicKeyAuthenticationExists();
        int keyIcon = hasPublicKey ? R.drawable.key_black_24dp : R.drawable.key_off_black_24dp;
        views.keyBasedAuthentication.setImageResource(keyIcon);

        if (hasPublicKey) {
            handlePublicKeyAuthentication(enable);
        } else {
            handleNoPublicKeyAuthentication(enable);
        }
    }

    private void handlePublicKeyAuthentication(boolean enable) {
        if (views.passwordAuthenticationEnabled.isChecked()) {
            setPasswordGroupVisibility(View.VISIBLE);
            enablePasswordAuthentication(enable, true);
        } else {
            setPasswordGroupVisibility(View.GONE);
            enablePasswordAuthentication(enable, false);
        }
    }

    private void handleNoPublicKeyAuthentication(boolean enable) {
        setPasswordGroupVisibility(View.VISIBLE);
        enablePasswordAuthentication(enable && publicKeyAuthenticationExists(), true);
    }

    private void updateActionButton(boolean enable) {
        int iconResource = enable ? R.drawable.play_arrow_black_24dp : R.drawable.pause_black_24dp;
        views.startStopAction.setImageResource(iconResource);
    }

    // Authentication Management
    private void setPasswordGroupVisibility(int visibility) {
        views.userLayout.setVisibility(visibility);
        views.passwordLayout.setVisibility(visibility);
        views.generate.setVisibility(visibility);
    }

    private void enablePasswordAuthentication(boolean enabled, boolean activated) {
        views.passwordAuthenticationEnabled.setEnabled(enabled);
        views.passwordAuthenticationEnabled.setChecked(activated);
        views.passwordAuthenticationEnabled.setActivated(activated);
    }

    // Server Status Management
    private void updateServerStatusDisplay(boolean enable) {
        if (!enable) {
            String statusText = getServerStatusText();
            views.serverStatusSummary.setText(statusText);
            views.serverStatusCard.setVisibility(View.VISIBLE);
        } else {
            views.serverStatusCard.setVisibility(View.GONE);
        }
    }

    private String getServerStatusText() {
        if (selectedInterface == null) {
            return buildAllInterfacesStatusText();
        } else {
            return "Server active on: " + selectedInterface;
        }
    }

    private String buildAllInterfacesStatusText() {
        var activeInterfaces = getActiveNetworkInterfaces();
        if (activeInterfaces.isEmpty()) {
            return "Server active on all available interfaces";
        } else {
            int count = activeInterfaces.size();
            String interfaceList = String.join(", ", activeInterfaces);
            String plural = count > 1 ? "s" : "";
            return "Server active on: " + interfaceList + " (" + count + " interface" + plural + ")";
        }
    }

    private List<String> getActiveNetworkInterfaces() {
        var adapter = (ArrayAdapter<String>) views.networkInterfaceSpinner.getAdapter();
        List<String> activeInterfaces = new ArrayList<>();

        if (adapter != null && adapter.getCount() > 1) {
            // Skip the first item which is "all interfaces"
            for (int i = 1; i < adapter.getCount(); i++) {
                activeInterfaces.add(adapter.getItem(i));
            }
        }
        return activeInterfaces;
    }

    // Fingerprint Management
    private void setFingerPrints(Map<SshFingerprint.DIGESTS, String> fingerPrints) {
        views.serverFingerprints.removeAllViews();
        addFingerprintHeader();
        addFingerprintEntries(fingerPrints);
    }

    private void addFingerprintHeader() {
        var headerText = createFingerprintHeaderTextView();
        var layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        views.serverFingerprints.addView(headerText, layoutParams);
    }

    private TextView createFingerprintHeaderTextView() {
        var textView = new TextView(this);
        textView.setEllipsize(END);
        textView.setSingleLine();
        textView.setMaxLines(1);
        textView.setTextSize(11);
        textView.setText(R.string.fingerprints_label_text);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        textView.setTypeface(null, Typeface.BOLD);
        return textView;
    }

    private void addFingerprintEntries(Map<SshFingerprint.DIGESTS, String> fingerPrints) {
        var layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );

        for (Map.Entry<SshFingerprint.DIGESTS, String> entry : fingerPrints.entrySet()) {
            var textView = createTextView(this, "(" + entry.getKey() + ") " + entry.getValue());
            views.serverFingerprints.addView(textView, layoutParams);
        }
    }

    // Spinner Management
    private void createSpinnerAdapter(Spinner sftpRootPaths) {
        if (isNull(sftpRootPaths.getSelectedItem())) {
            var adapter = new ArrayAdapter<>(this, R.layout.spinner_item, getAllStorageLocations(this));
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            sftpRootPaths.setAdapter(adapter);
        }
    }

    // Preferences Management
    private void storeValues(String selectedInterface, String port, String user,
                             boolean passwordAuthenticationEnabled, boolean readOnly, String sftpRootPath) {
        var editor = getPreferences(Context.MODE_PRIVATE).edit();

        editor.putString(getString(R.string.select_network_interface), selectedInterface);
        editor.putString(getString(R.string.default_port_value), port);
        editor.putString(getString(R.string.default_user_value), user);
        editor.putString(getString(R.string.sftp_root_path), sftpRootPath);
        editor.putBoolean(getString(R.string.password_authentication_enabled), passwordAuthenticationEnabled);
        editor.putBoolean(getString(R.string.read_only), readOnly);

        editor.apply();
    }

    private void restoreValues() {
        var preferences = getPreferences(Context.MODE_PRIVATE);

        restoreNetworkInterface(preferences);
        restoreTextFields(preferences);
        restoreSwitches(preferences);
        restoreSftpPath(preferences);
    }

    private void restoreNetworkInterface(android.content.SharedPreferences preferences) {
        setSelectedInterface(preferences.getString(getString(R.string.select_network_interface), null));
        var adapter = (ArrayAdapter<String>) views.networkInterfaceSpinner.getAdapter();
        var position = adapter.getPosition(selectedInterface);

        if (position >= 0) {
            views.networkInterfaceSpinner.setSelection(position);
        }
    }

    private void restoreTextFields(android.content.SharedPreferences preferences) {
        var defaultPort = getString(R.string.default_port_value);
        var defaultUser = getString(R.string.default_user_value);

        views.portValue.setText(preferences.getString(getString(R.string.default_port_value), defaultPort));
        views.userValue.setText(preferences.getString(getString(R.string.default_user_value), defaultUser));
    }

    private void restoreSwitches(android.content.SharedPreferences preferences) {
        views.passwordAuthenticationEnabled.setChecked(
                preferences.getBoolean(getString(R.string.password_authentication_enabled), true)
        );
        views.readonly.setChecked(
                preferences.getBoolean(getString(R.string.read_only), false)
        );
    }

    private void restoreSftpPath(android.content.SharedPreferences preferences) {
        createSpinnerAdapter(views.sftpPaths);
        var savedPath = preferences.getString(getString(R.string.sftp_root_path), "/");
        var adapter = (ArrayAdapter<String>) views.sftpPaths.getAdapter();
        var position = adapter.getPosition(savedPath);
        views.sftpPaths.setSelection(position);
    }

    // Permission Management
    private void setupPermissions() {
        setupStoragePermissions();
        setupNotificationPermissions();
    }

    private void setupStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        } else if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void setupNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !getSystemService(NotificationManager.class).areNotificationsEnabled()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    // Service Status Management
    private boolean isStarted() {
        var am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        @SuppressWarnings("deprecation")
        var runningServices = am.getRunningServices(1);
        var started = false;
        if (!runningServices.isEmpty() && runningServices.get(0).service.flattenToString().contains(SSH_DAEMON)) {
            started = runningServices.get(0).started;
        }
        return started;
    }

    private void updateViews() {
        enableViews(!isStarted());
    }

    // Utility Methods
    private String getValue(EditText editText) {
        return editText.getText().toString().isEmpty() ?
                editText.getHint().toString() :
                editText.getText().toString();
    }

    // Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        views = new ViewHolder(this);
        setupNetworkChangeReceiver();
        setupPermissions();
        setupWindowInsets();
        initializeApp();
    }

    private void setupNetworkChangeReceiver() {
        var connectivityManager = getSystemService(ConnectivityManager.class);
        connectivityManager.registerDefaultNetworkCallback(
                new NetworkChangeReceiver(views.networkInterfaceSpinner, connectivityManager, this)
        );
    }

    private void setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (view, insets) -> {
            var systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBarsInsets.left, systemBarsInsets.top,
                    systemBarsInsets.right, systemBarsInsets.bottom);
            return insets;
        });
    }

    private void initializeApp() {
        setFingerPrints(getFingerPrints());
        generateClicked(null);
        restoreValues();
        updateViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreValues();
        updateViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        var notificationManager = getSystemService(NotificationManager.class);
        if (!isNull(notificationManager)) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    // Public Interface Methods
    public void setSelectedInterface(String selectedInterface) {
        this.selectedInterface = selectedInterface;
    }

    // Click Handlers
    public void keyClicked(View view) {
        var text = publicKeyAuthenticationExists() ?
                getResources().getString(R.string.ssh_public_key_exists) :
                getResources().getString(R.string.ssh_public_key_doesnt_exists);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void generateClicked(View view) {
        var passwordField = (TextInputEditText) views.passwordValue;
        passwordField.setText(getRandomString(6));
    }

    public void passwordSwitchClicked(View passwordAuthenticationEnabled) {
        var passwordSwitch = (SwitchMaterial) passwordAuthenticationEnabled;
        enablePasswordAuthentication(true, !passwordSwitch.isActivated());
    }

    public void startStopClicked(View view) {
        if (isStarted()) {
            handleStopService();
        } else {
            handleStartService();
        }
    }

    private void handleStopService() {
        enableViews(true);
        stopService();
    }

    private void handleStartService() {
        enableViews(false);
        setFingerPrints(getFingerPrints());

        var serviceParams = collectServiceParameters();
        storeValues(selectedInterface, serviceParams.port, serviceParams.user,
                serviceParams.passwordAuthEnabled, serviceParams.readOnly, serviceParams.sftpRootPath);

        startService(Integer.parseInt(serviceParams.port), serviceParams.user, serviceParams.password,
                serviceParams.sftpRootPath, serviceParams.passwordAuthEnabled, serviceParams.readOnly);
    }

    private ServiceParameters collectServiceParameters() {
        var port = getValue(views.portValue);
        var user = getValue(views.userValue);
        var password = getValue(views.passwordValue);
        var sftpRootPath = views.sftpPaths.getSelectedItem().toString();
        var passwordAuthEnabled = views.passwordAuthenticationEnabled.isChecked();
        var readOnly = views.readonly.isChecked();

        return new ServiceParameters(port, user, password, sftpRootPath, passwordAuthEnabled, readOnly);
    }

    private record ServiceParameters(String port, String user, String password, String sftpRootPath,
                                     boolean passwordAuthEnabled, boolean readOnly) {
    }

    // Service Management
    public void startService(int port, String user, String password, String sftpRootPath,
                             boolean passwordAuthenticationEnabled, boolean readOnly) {
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
