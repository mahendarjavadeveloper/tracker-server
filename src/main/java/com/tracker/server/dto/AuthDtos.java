package com.tracker.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record UserRequest(
            @NotBlank @Size(max = 100) String username
    ) {
    }

    public record AdminRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(min = 6, max = 200) String password
    ) {
    }

    public record AuthResponse(
            String token,
            Long userId,
            String username,
            String role
    ) {
    }
}
