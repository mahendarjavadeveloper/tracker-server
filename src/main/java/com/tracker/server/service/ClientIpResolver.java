package com.tracker.server.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ClientIpResolver {
    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            for (String candidate : forwardedFor.split(",")) {
                String publicIp = normalizePublicIp(candidate);
                if (publicIp != null) {
                    return publicIp;
                }
            }
        }

        String realIp = normalizePublicIp(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalizePublicIp(request.getRemoteAddr());
    }

    private String normalizePublicIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String candidate = value.trim();
        if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() > 1) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.startsWith("[")) {
            int bracket = candidate.indexOf(']');
            candidate = bracket > 0 ? candidate.substring(1, bracket) : candidate;
        } else if (candidate.indexOf(':') == candidate.lastIndexOf(':') && candidate.contains(".")) {
            int colon = candidate.lastIndexOf(':');
            if (colon > 0) {
                candidate = candidate.substring(0, colon);
            }
        }

        int zone = candidate.indexOf('%');
        if (zone > 0) {
            candidate = candidate.substring(0, zone);
        }
        if (!isIpLiteral(candidate)) {
            return null;
        }

        try {
            InetAddress address = InetAddress.getByName(candidate);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()
                    || isUniqueLocalIpv6(address)) {
                return null;
            }
            return address.getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private boolean isIpLiteral(String value) {
        if (value.contains(":")) {
            return value.matches("[0-9a-fA-F:.]+") && value.length() <= 64;
        }
        return value.matches("(?:\\d{1,3}\\.){3}\\d{1,3}");
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        int firstByte = address.getAddress()[0] & 0xff;
        return (firstByte & 0xfe) == 0xfc;
    }
}
