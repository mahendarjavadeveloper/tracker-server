package com.tracker.server.security;

public record TrackerPrincipal(Long userId, String username, String role) {
}
