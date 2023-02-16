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

    public NetworkChangeReceiver(LinearLayout networkInterfaces, Context context) {
        this.networkInterfaces = networkInterfaces;
        showNetworkInterfaces(context);
    }

    static Set<String> getInterfaces() {
        var result = new TreeSet<String>();

        try {

            var networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {

                var networkInterface = networkInterfaces.nextElement();

                if ((!networkInterface.isLoopback()) &&
                        networkInterface.isUp() &&
                        !networkInterface.isVirtual()) {

                    var addresses = networkInterface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        var inetAddress = addresses.nextElement();
                        var hostAddress = inetAddress.getHostAddress();
                        if (!isNull(hostAddress) && !(hostAddress.contains("dummy") || hostAddress.contains("rmnet"))) {
                            hostAddress = hostAddress.replace("%", " on interface ");
                        }
                        result.add(hostAddress);
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Exception: " + e);
        }
        return result;
    }

    private boolean hasConnectivity(ConnectivityManager connectivityManager) {
        if (isNull(connectivityManager))
            return false;

        var nw = connectivityManager.getActiveNetwork();

        if (isNull(nw)) return false;

        var actNw = connectivityManager.getNetworkCapabilities(nw);

        return !isNull(actNw) &&
                (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    private void showNetworkInterfaces(Context context) {

        this.networkInterfaces.removeAllViews();

        if (!hasConnectivity((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)))
            return;

        var interfacesText = new TextView(context);
        interfacesText.setText(R.string.interface_label_text);
        interfacesText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        interfacesText.setTypeface(null, Typeface.BOLD);

        networkInterfaces.addView(interfacesText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        for (var interfaceAddress : getInterfaces())
            networkInterfaces.addView(createTextView(context, interfaceAddress),
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        showNetworkInterfaces(context);
    }
}