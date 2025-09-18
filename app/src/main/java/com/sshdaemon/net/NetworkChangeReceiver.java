package com.sshdaemon.net;

import static com.sshdaemon.util.AndroidLogger.getLogger;
import static java.util.Objects.isNull;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.sshdaemon.MainActivity;

import org.slf4j.Logger;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class NetworkChangeReceiver extends ConnectivityManager.NetworkCallback {

    private static final Logger logger = getLogger();
    private final Spinner networkInterfaces;
    private final MainActivity activity;
    private final ConnectivityManager connectivityManager;

    public NetworkChangeReceiver(Spinner networkInterfaces, ConnectivityManager connectivityManager, MainActivity activity) {
        this.networkInterfaces = networkInterfaces;
        this.connectivityManager = connectivityManager;
        this.activity = activity;

        networkInterfaces.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                synchronized (NetworkChangeReceiver.this) {
                    if (position == 0) {
                        activity.setSelectedInterface(null);
                    } else {
                        activity.setSelectedInterface(getInterfaces().get(position));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                activity.setSelectedInterface(null);
            }
        });

        setAdapter();
    }

    private void setAdapter() {
        try {
            synchronized (this) {
                var interfaces = getInterfaces();
                activity.runOnUiThread(() -> {
                    var adapter = (ArrayAdapter<String>) networkInterfaces.getAdapter();
                    if (!isNull(adapter)) {
                        adapter.clear();
                        adapter.addAll(interfaces);
                        adapter.notifyDataSetChanged();
                    } else {
                        adapter = new ArrayAdapter<>(activity, com.sshdaemon.R.layout.spinner_item, interfaces);
                        adapter.setDropDownViewResource(com.sshdaemon.R.layout.spinner_dropdown_item);
                        networkInterfaces.setAdapter(adapter);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error setting adapter for network interfaces: ", e);
        }
    }


    List<String> getInterfaces() {
        var result = new TreeSet<String>();
        if (!hasConnectivity()) {
            logger.warn("No connectivity detected.");
            return Collections.emptyList();
        }

        try {
            var networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                var networkInterface = networkInterfaces.nextElement();
                if (isValidInterface(networkInterface)) {
                    addInterfaceAddresses(result, networkInterface);
                }
            }
        } catch (SocketException e) {
            logger.error("Exception while fetching network interfaces: ", e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching network interfaces: ", e);
        }

        var interfaces = new ArrayList<>(result);
        interfaces.add(0, "all interfaces"); // Default option
        return interfaces;
    }

    private static boolean isValidInterface(NetworkInterface networkInterface) {
        try {
            return !networkInterface.isLoopback() && networkInterface.isUp() && !networkInterface.isVirtual();
        } catch (SocketException e) {
            logger.error("Error checking validity of network interface: ", e);
            return false;
        }
    }

    private static void addInterfaceAddresses(Set<String> result, NetworkInterface networkInterface) {
        try {
            var addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                var inetAddress = addresses.nextElement();
                var hostAddress = inetAddress.getHostAddress();

                if (!isNull(hostAddress) && !(hostAddress.contains("dummy") || hostAddress.contains("rmnet"))) {
                    result.add(hostAddress.split("%")[0]); // Exclude scope ID for IPv6
                }
            }
        } catch (Exception e) {
            logger.error("Error adding interface addresses: ", e);
        }
    }

    private boolean hasConnectivity() {
        try {
            var nw = connectivityManager.getActiveNetwork();
            if (isNull(nw)) return false;
            var actNw = connectivityManager.getNetworkCapabilities(nw);
            return !isNull(actNw) && (
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } catch (Exception e) {
            logger.error("Error checking connectivity: ", e);
            return false;
        }
    }

    @Override
    public void onAvailable(@NonNull android.net.Network network) {
        super.onAvailable(network);
        logger.info("Network available. Updating network interfaces.");
        setAdapter();
    }

    @Override
    public void onLost(@NonNull android.net.Network network) {
        super.onLost(network);
        logger.info("Network lost. Updating network interfaces.");
        setAdapter();
    }
}
