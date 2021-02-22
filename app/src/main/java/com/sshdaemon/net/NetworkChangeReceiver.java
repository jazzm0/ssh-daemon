package com.sshdaemon.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sshdaemon.R;


import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import static com.sshdaemon.util.AndroidLogger.getLogger;
import static com.sshdaemon.util.TextViewHelper.createTextView;
import static java.util.Objects.isNull;


public class NetworkChangeReceiver extends BroadcastReceiver {

    private final LinearLayout networkInterfaces;
    private final Logger logger = getLogger();

    public NetworkChangeReceiver(LinearLayout networkInterfaces, Context context) {
        this.networkInterfaces = networkInterfaces;
        showNetworkInterfaces(context);
    }

    private Set<String> getInterfaces() {
        TreeSet<String> result = new TreeSet<>();

        try {

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {

                NetworkInterface networkInterface = networkInterfaces.nextElement();

                if ((!networkInterface.isLoopback()) &&
                        networkInterface.isUp() &&
                        !networkInterface.isVirtual()) {

                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress inetAddress = addresses.nextElement();
                        String hostAddress = inetAddress.getHostAddress();
                        if (!(hostAddress.contains("dummy") || hostAddress.contains("rmnet"))) {
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

        Network nw = connectivityManager.getActiveNetwork();

        if (isNull(nw)) return false;

        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);

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

        TextView interfacesText = new TextView(context);
        interfacesText.setText(R.string.interface_label_text);
        interfacesText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        interfacesText.setTypeface(null, Typeface.BOLD);

        networkInterfaces.addView(interfacesText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        for (String interfaceAddress : getInterfaces())
            networkInterfaces.addView(createTextView(context, interfaceAddress),
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        showNetworkInterfaces(context);
    }
}