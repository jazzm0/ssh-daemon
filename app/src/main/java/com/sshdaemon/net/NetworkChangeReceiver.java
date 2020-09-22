package com.sshdaemon.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.sshdaemon.R;


public class NetworkChangeReceiver extends BroadcastReceiver {

    private final LinearLayout networkInterfaces;

    public NetworkChangeReceiver(LinearLayout networkInterfaces, Context context) {
        this.networkInterfaces = networkInterfaces;
        showNetworkInterfaces(context);
    }

    private boolean hasConnectivity(ConnectivityManager connectivityManager) {
        Network nw = connectivityManager.getActiveNetwork();

        if (nw == null) return false;

        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);

        return actNw != null &&
                (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    private TextView createTextView(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        return textView;
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

        NetworkInformation networkInformation = new NetworkInformation();

        for (String interfaceAddress : networkInformation.getInfo())
            networkInterfaces.addView(createTextView(context, interfaceAddress),
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        showNetworkInterfaces(context);
    }
}