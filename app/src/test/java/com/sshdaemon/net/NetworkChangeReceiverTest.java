package com.sshdaemon.net;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.view.View;
import android.widget.LinearLayout;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NetworkChangeReceiverTest {

    private final LinearLayout linearLayout = mock(LinearLayout.class);
    private final Context context = mock(Context.class);
    private final NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver(linearLayout, context);

    @Test
    public void testNoConnectivity() {
        networkChangeReceiver.onReceive(context, mock(Intent.class));
    }

    @Test
    public void testViewsAreAdded() {
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        Network network = mock(Network.class);
        NetworkCapabilities networkCapabilities = mock(NetworkCapabilities.class);
        when(networkCapabilities.hasTransport(anyInt())).thenReturn(true);
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        networkChangeReceiver.onReceive(context, mock(Intent.class));

        ArgumentCaptor<View> view = ArgumentCaptor.forClass(View.class);
        ArgumentCaptor<LinearLayout.LayoutParams> layout = ArgumentCaptor.forClass(LinearLayout.LayoutParams.class);

        verify(linearLayout, times(5)).addView(view.capture(), layout.capture());
    }
}