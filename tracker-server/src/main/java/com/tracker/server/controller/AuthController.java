package com.tracker.server.controller;

import com.tracker.server.dto.AuthDtos.AdminRequest;
import com.tracker.server.dto.AuthDtos.AuthResponse;
import com.tracker.server.dto.AuthDtos.UserRequest;
import com.tracker.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerUser(@Valid @RequestBody UserRequest request) {
        return authService.registerUser(request);
    }

    @PostMapping("/user/login")
    public AuthResponse loginUser(@Valid @RequestBody UserRequest request) {
        return authService.loginUser(request);
    }

    @PostMapping("/admin/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerAdmin(@Valid @RequestBody AdminRequest request) {
        return authService.registerAdmin(request);
    }

    @PostMapping("/admin/login")
    public AuthResponse loginAdmin(@Valid @RequestBody AdminRequest request) {
        return authService.loginAdmin(request);
    }
}
