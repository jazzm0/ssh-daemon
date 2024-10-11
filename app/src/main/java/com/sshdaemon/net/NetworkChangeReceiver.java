package com.sshdaemon.net;

import static com.sshdaemon.util.AndroidLogger.getLogger;
import static com.sshdaemon.util.TextViewHelper.createTextView;
import static java.util.Objects.isNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sshdaemon.R;

import org.slf4j.Logger;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Set;
import java.util.TreeSet;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final Logger logger = getLogger();
    private final LinearLayout networkInterfaces;
    private final ConnectivityManager connectivityManager;

    public NetworkChangeReceiver(LinearLayout networkInterfaces, ConnectivityManager connectivityManager) {
        this.networkInterfaces = networkInterfaces;
        this.connectivityManager = connectivityManager;
        showNetworkInterfaces();
    }

    static Set<String> getInterfaces() {
        var result = new TreeSet<String>();

        try {
            var networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                var networkInterface = networkInterfaces.nextElement();
                if (isValidInterface(networkInterface)) {
                    addInterfaceAddresses(result, networkInterface);
                }
            }
        } catch (SocketException e) {
            logger.error("Exception: ", e);
        }
        return result;
    }

    private static boolean isValidInterface(NetworkInterface networkInterface) throws SocketException {
        return !networkInterface.isLoopback() && networkInterface.isUp() && !networkInterface.isVirtual();
    }

    private static void addInterfaceAddresses(Set<String> result, NetworkInterface networkInterface) {
        var addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
            var inetAddress = addresses.nextElement();
            var hostAddress = inetAddress.getHostAddress();
            if (!isNull(hostAddress) && !(hostAddress.contains("dummy") || hostAddress.contains("rmnet"))) {
                result.add(hostAddress.replace("%", " on interface "));
            }
        }
    }

    private boolean hasConnectivity() {
        var nw = connectivityManager.getActiveNetwork();
        if (isNull(nw)) return false;
        var actNw = connectivityManager.getNetworkCapabilities(nw);
        return !isNull(actNw) && (
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    private void showNetworkInterfaces() {
        networkInterfaces.removeAllViews();
        if (!hasConnectivity()) return;

        var interfacesText = new TextView(networkInterfaces.getContext());
        interfacesText.setText(R.string.interface_label_text);
        interfacesText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        interfacesText.setTypeface(null, Typeface.BOLD);

        networkInterfaces.addView(interfacesText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        for (var interfaceAddress : getInterfaces()) {
            networkInterfaces.addView(createTextView(networkInterfaces.getContext(), interfaceAddress),
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        showNetworkInterfaces();
    }
}