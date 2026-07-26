package com.tracker.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class DeviceDtos {
    private DeviceDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 64) String installationId,
            @Size(max = 64) String macAddress,
            @NotBlank @Size(max = 255) String machineName,
            @Size(max = 255) String osName,
            @Size(max = 64) String ipAddress
    ) {
    }

    public record HeartbeatRequest(
            LocalDateTime observedAt,
            @Size(max = 64) String ipAddress
    ) {
    }

    public record ShutdownRequest(LocalDateTime shutdownAt) {
    }

    public record UninstallRequest(LocalDateTime uninstalledAt) {
    }
}
