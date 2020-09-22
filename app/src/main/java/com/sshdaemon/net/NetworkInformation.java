package com.sshdaemon.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetworkInformation {

    public List<String> getInfo() {

        List<String> result = new ArrayList<>();
        List<String> ipv4Addresses = new ArrayList<>();
        List<String> ipv6Addresses = new ArrayList<>();
        List<NetworkInterface> interfaces;

        try {
            interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if ((!networkInterface.isLoopback()) && networkInterface.isUp() && !networkInterface.isVirtual()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    for (InetAddress inetAddress : Collections.list(addresses)) {
                        Inet4Address ipv4Address = null;
                        Inet6Address ipv6Address = null;
                        String hostAddress = null;
                        try {
                            ipv4Address = (Inet4Address) inetAddress;
                            hostAddress = ipv4Address.getHostAddress();
                            ipv4Addresses.add(hostAddress);
                        } catch (ClassCastException e) {
                        }
                        try {
                            ipv6Address = (Inet6Address) inetAddress;
                            hostAddress = ipv6Address.getHostAddress();
                            if (!(hostAddress.contains("dummy") || hostAddress.contains("rmnet"))) {
                                ipv6Addresses.add(hostAddress.replace("%", " on interface "));
                            }
                        } catch (ClassCastException e) {
                        }
                    }
                }
            }
            for (String ipv4 : ipv4Addresses) {
                result.add(ipv4);
            }
            for (String ipv6 : ipv6Addresses) {
                result.add(ipv6);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return result;
    }
}
