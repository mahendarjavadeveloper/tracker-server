package com.tracker.server.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {
    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String ipAddress = first(request.getHeader("X-Forwarded-For"));
        if (ipAddress == null) {
            ipAddress = forwarded(request.getHeader("Forwarded"));
        }
        if (ipAddress == null) {
            ipAddress = normalize(request.getHeader("X-Real-IP"));
        }
        if (ipAddress == null) {
            ipAddress = normalize(request.getRemoteAddr());
        }
        return ipAddress;
    }

    private static String first(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return normalize(header.split(",", 2)[0]);
    }

    private static String forwarded(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        for (String parameter : header.split(",", 2)[0].split(";")) {
            int separator = parameter.indexOf('=');
            if (separator > 0 && "for".equalsIgnoreCase(parameter.substring(0, separator).trim())) {
                return normalize(parameter.substring(separator + 1));
            }
        }
        return null;
    }

    private static String normalize(String source) {
        if (source == null) {
            return null;
        }
        String value = source.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.equalsIgnoreCase("unknown") || value.startsWith("_")) {
            return null;
        }
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            if (end < 2) {
                return null;
            }
            value = value.substring(1, end);
        } else if (value.contains(".") && value.indexOf(':') == value.lastIndexOf(':')) {
            int port = value.lastIndexOf(':');
            if (port > 0) {
                value = value.substring(0, port);
            }
        }
        int zone = value.indexOf('%');
        if (zone > 0) {
            value = value.substring(0, zone);
        }
        if (value.isBlank()
                || value.length() > 64
                || !value.matches("[0-9A-Fa-f:.]+")) {
            return null;
        }
        return value;
    }
}
