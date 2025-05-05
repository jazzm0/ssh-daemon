package com.sshdaemon.net;

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
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.sshdaemon.MainActivity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NetworkChangeReceiverTest {

    private final Spinner spinner = mock(Spinner.class);
    private final MainActivity mainActivity = mock(MainActivity.class);
    private NetworkChangeReceiver networkChangeReceiver;
    private ConnectivityManager connectivityManager;

    @BeforeEach
    void setup() {
        connectivityManager = mock(ConnectivityManager.class);
        networkChangeReceiver = new NetworkChangeReceiver(spinner, connectivityManager, mainActivity);
        reset(spinner);
        reset(mainActivity);
    }

    @Test
    void testNoConnectivity() {
        networkChangeReceiver.onReceive(mainActivity, mock(Intent.class));
        verify(spinner, times(0)).addView(any(), any());
    }

    @Test
    void testSpinnerIsUpdated() {
        var network = mock(Network.class);
        var networkCapabilities = mock(NetworkCapabilities.class);
        when(networkCapabilities.hasTransport(anyInt())).thenReturn(true);
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        networkChangeReceiver.onReceive(mainActivity, mock(Intent.class));

        var spinnerAdapter = ArgumentCaptor.forClass(SpinnerAdapter.class);

        final var interfaces = networkChangeReceiver.getInterfaces();

        assertFalse(interfaces.isEmpty());

        verify(spinner, times(1)).setAdapter(spinnerAdapter.capture());
    }
}