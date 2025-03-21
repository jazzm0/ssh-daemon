package com.sshdaemon.net;

import static com.sshdaemon.net.NetworkChangeReceiver.getInterfaces;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.view.View;
import android.widget.LinearLayout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NetworkChangeReceiverTest {

    private final LinearLayout linearLayout = mock(LinearLayout.class);
    private final Context context = mock(Context.class);
    private NetworkChangeReceiver networkChangeReceiver;
    private ConnectivityManager connectivityManager;

    @BeforeEach
    void setup() {
        connectivityManager = mock(ConnectivityManager.class);
        networkChangeReceiver = new NetworkChangeReceiver(linearLayout, connectivityManager);
        reset(linearLayout);
        reset(context);
    }

    @Test
    void testNoConnectivity() {
        networkChangeReceiver.onReceive(context, mock(Intent.class));
        verify(linearLayout, times(0)).addView(any(), any());
    }

    @Test
    void testViewsAreAdded() {
        var network = mock(Network.class);
        var networkCapabilities = mock(NetworkCapabilities.class);
        when(networkCapabilities.hasTransport(anyInt())).thenReturn(true);
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        networkChangeReceiver.onReceive(context, mock(Intent.class));

        var view = ArgumentCaptor.forClass(View.class);
        var layout = ArgumentCaptor.forClass(LinearLayout.LayoutParams.class);

        final var interfaces = getInterfaces();

        assertFalse(interfaces.isEmpty());

        verify(linearLayout, times(interfaces.size() + 1)).addView(view.capture(), layout.capture());
    }
}