package com.sshdaemon.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

public class NetworkInformation {

    public Set<String> getInfo() {

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
            e.printStackTrace();
        }
        return result;
    }
}
