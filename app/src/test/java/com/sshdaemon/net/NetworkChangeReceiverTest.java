package com.sshdaemon.net;

import static com.sshdaemon.net.NetworkChangeReceiver.getInterfaces;
import static org.junit.Assert.assertFalse;
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class NetworkChangeReceiverTest {

    private final LinearLayout linearLayout = mock(LinearLayout.class);
    private final Context context = mock(Context.class);
    private final NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver(linearLayout, context);

    @Before
    public void setup() {
        reset(linearLayout);
        reset(context);
    }

    @Test
    public void testNoConnectivity() {
        networkChangeReceiver.onReceive(context, mock(Intent.class));
        verify(linearLayout, times(0)).addView(any(), any());
    }

    @Test
    public void testViewsAreAdded() {
        var connectivityManager = mock(ConnectivityManager.class);
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