package com.tracker.server.service;

import com.tracker.server.dto.AuthDtos.AdminRequest;
import com.tracker.server.dto.AuthDtos.AuthResponse;
import com.tracker.server.dto.AuthDtos.UserRequest;
import com.tracker.server.entity.User;
import com.tracker.server.repository.UserRepository;
import com.tracker.server.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse registerUser(UserRequest request) {
        String username = normalize(request.username());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setRole("USER");
        return response(userRepository.save(user));
    }

    public AuthResponse loginUser(UserRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(normalize(request.username()))
                .filter(value -> "USER".equals(value.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));
        return response(user);
    }

    public AuthResponse registerAdmin(AdminRequest request) {
        String username = normalize(request.username());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole("ADMIN");
        return response(userRepository.save(admin));
    }

    public AuthResponse loginAdmin(AdminRequest request) {
        User admin = userRepository.findByUsernameIgnoreCase(normalize(request.username()))
                .filter(value -> "ADMIN".equals(value.getRole()))
                .filter(value -> value.getPasswordHash() != null
                        && passwordEncoder.matches(request.password(), value.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return response(admin);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(
                jwtService.create(user),
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    private String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
