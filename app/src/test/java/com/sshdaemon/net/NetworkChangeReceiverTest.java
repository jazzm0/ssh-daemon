package com.sshdaemon.net;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.sshdaemon.MainActivity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class NetworkChangeReceiverTest {

    @Mock
    private Spinner mockSpinner;

    @Mock
    private MainActivity mockActivity;

    @Mock
    private ConnectivityManager mockConnectivityManager;

    @Mock
    private Network mockNetwork;

    @Mock
    private NetworkCapabilities mockNetworkCapabilities;

    @Mock
    private ArrayAdapter<String> mockAdapter;

    private NetworkChangeReceiver networkChangeReceiver;

    @BeforeEach
    void setup() {
        // Setup default behavior for runOnUiThread to execute immediately
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockActivity).runOnUiThread(any(Runnable.class));

        // Create the receiver under test
        networkChangeReceiver = new NetworkChangeReceiver(mockSpinner, mockConnectivityManager, mockActivity);
    }

    @Test
    void testConstructorSetsUpSpinnerListener() {
        // Verify that the spinner's item selected listener is set
        verify(mockSpinner).setOnItemSelectedListener(any(AdapterView.OnItemSelectedListener.class));
    }

    @Test
    void testOnAvailableUpdatesNetworkInterfaces() {
        // Given: Network connectivity is available
        setupNetworkConnectivity(true);
        when(mockSpinner.getAdapter()).thenReturn(mockAdapter);

        // When: Network becomes available
        networkChangeReceiver.onAvailable(mockNetwork);

        // Then: UI thread is used to update the adapter
        verify(mockActivity, atLeastOnce()).runOnUiThread(any(Runnable.class));
        verify(mockAdapter).clear();
        verify(mockAdapter).addAll(any(List.class));
        verify(mockAdapter).notifyDataSetChanged();
    }

    @Test
    void testOnLostUpdatesNetworkInterfaces() {
        // Given: Network connectivity is lost
        setupNetworkConnectivity(false);
        when(mockSpinner.getAdapter()).thenReturn(mockAdapter);

        // When: Network is lost
        networkChangeReceiver.onLost(mockNetwork);

        // Then: UI thread is used to update the adapter
        verify(mockActivity, atLeastOnce()).runOnUiThread(any(Runnable.class));
        verify(mockAdapter).clear();
        verify(mockAdapter).addAll(any(List.class));
        verify(mockAdapter).notifyDataSetChanged();
    }

    @Test
    void testGetInterfacesReturnsEmptyListWhenNoConnectivity() {
        // Given: No network connectivity
        setupNetworkConnectivity(false);

        // When: Getting interfaces
        List<String> interfaces = networkChangeReceiver.getInterfaces();

        // Then: Empty list is returned
        assertTrue(interfaces.isEmpty());
    }

    @Test
    void testGetInterfacesReturnsDefaultWhenConnectivityButNoInterfaces() {
        // Given: Network connectivity is available but no valid interfaces
        setupNetworkConnectivity(true);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = mockStatic(NetworkInterface.class)) {
            @SuppressWarnings("unchecked")
            Enumeration<NetworkInterface> emptyEnumeration = mock(Enumeration.class);
            when(emptyEnumeration.hasMoreElements()).thenReturn(false);

            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenReturn(emptyEnumeration);

            // When: Getting interfaces
            List<String> interfaces = networkChangeReceiver.getInterfaces();

            // Then: Only default "all interfaces" option is returned
            assertEquals(1, interfaces.size());
            assertEquals("all interfaces", interfaces.get(0));
        }
    }

    @Test
    void testGetInterfacesHandlesSocketException() {
        // Given: Network connectivity is available but NetworkInterface throws exception
        setupNetworkConnectivity(true);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = mockStatic(NetworkInterface.class)) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenThrow(new SocketException("Test exception"));

            // When: Getting interfaces
            List<String> interfaces = networkChangeReceiver.getInterfaces();

            // Then: Only default "all interfaces" option is returned
            assertEquals(1, interfaces.size());
            assertEquals("all interfaces", interfaces.get(0));
        }
    }

    @Test
    void testGetInterfacesHandlesGeneralException() {
        // Given: Network connectivity is available but NetworkInterface throws general exception
        setupNetworkConnectivity(true);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = mockStatic(NetworkInterface.class)) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When: Getting interfaces
            List<String> interfaces = networkChangeReceiver.getInterfaces();

            // Then: Only default "all interfaces" option is returned
            assertEquals(1, interfaces.size());
            assertEquals("all interfaces", interfaces.get(0));
        }
    }

    @Test
    void testSetAdapterCreatesNewAdapterWhenNoneExists() {
        // Given: No existing adapter
        when(mockSpinner.getAdapter()).thenReturn(null);
        setupNetworkConnectivity(true);

        // When: Network becomes available (triggers setAdapter)
        networkChangeReceiver.onAvailable(mockNetwork);

        // Then: New adapter is created and set (allow multiple calls since constructor also triggers setAdapter)
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockActivity, atLeastOnce()).runOnUiThread(runnableCaptor.capture());

        // Execute the runnable to verify adapter creation
        runnableCaptor.getValue().run();
        verify(mockSpinner, atLeastOnce()).setAdapter(any(ArrayAdapter.class));
    }

    @Test
    void testSetAdapterHandlesExceptionGracefully() {
        // Given: Spinner adapter setup throws an exception
        when(mockSpinner.getAdapter()).thenThrow(new RuntimeException("Test exception"));
        setupNetworkConnectivity(true);

        // When: Network becomes available (triggers setAdapter)
        // Then: Exception is caught and doesn't propagate (test doesn't fail)
        assertDoesNotThrow(() -> networkChangeReceiver.onAvailable(mockNetwork));
    }

    @Test
    void testConnectivityCheckWithWifi() {
        // Given: WiFi network is available
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockNetworkCapabilities);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = mockStatic(NetworkInterface.class)) {
            @SuppressWarnings("unchecked")
            Enumeration<NetworkInterface> emptyEnumeration = mock(Enumeration.class);
            when(emptyEnumeration.hasMoreElements()).thenReturn(false);
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces).thenReturn(emptyEnumeration);

            // When: Getting interfaces
            List<String> interfaces = networkChangeReceiver.getInterfaces();

            // Then: Connectivity is detected and default interface is returned
            assertEquals(1, interfaces.size());
            assertEquals("all interfaces", interfaces.get(0));
        }
    }

    @Test
    void testConnectivityCheckWithCellular() {
        // Given: Cellular network is available
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockNetworkCapabilities);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = mockStatic(NetworkInterface.class)) {
            @SuppressWarnings("unchecked")
            Enumeration<NetworkInterface> emptyEnumeration = mock(Enumeration.class);
            when(emptyEnumeration.hasMoreElements()).thenReturn(false);
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces).thenReturn(emptyEnumeration);

            // When: Getting interfaces
            List<String> interfaces = networkChangeReceiver.getInterfaces();

            // Then: Connectivity is detected and default interface is returned
            assertEquals(1, interfaces.size());
            assertEquals("all interfaces", interfaces.get(0));
        }
    }

    @Test
    void testConnectivityCheckWithEthernet() {
        // Given: Ethernet network is available
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockNetworkCapabilities);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(false);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = mockStatic(NetworkInterface.class)) {
            @SuppressWarnings("unchecked")
            Enumeration<NetworkInterface> emptyEnumeration = mock(Enumeration.class);
            when(emptyEnumeration.hasMoreElements()).thenReturn(false);
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces).thenReturn(emptyEnumeration);

            // When: Getting interfaces
            List<String> interfaces = networkChangeReceiver.getInterfaces();

            // Then: Connectivity is detected and default interface is returned
            assertEquals(1, interfaces.size());
            assertEquals("all interfaces", interfaces.get(0));
        }
    }

    @Test
    void testConnectivityCheckHandlesException() {
        // Given: ConnectivityManager throws exception
        when(mockConnectivityManager.getActiveNetwork()).thenThrow(new RuntimeException("Test exception"));

        // When: Getting interfaces
        List<String> interfaces = networkChangeReceiver.getInterfaces();

        // Then: Empty list is returned (no connectivity detected)
        assertTrue(interfaces.isEmpty());
    }

    private void setupNetworkConnectivity(boolean hasConnectivity) {
        if (hasConnectivity) {
            when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
            when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockNetworkCapabilities);
            when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);
        } else {
            when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        }
    }
}
