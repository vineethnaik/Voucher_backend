package com.voucherpro.service;

import com.voucherpro.dto.AuthResponse;
import com.voucherpro.dto.LoginRequest;
import com.voucherpro.dto.RegisterRequest;
import com.voucherpro.model.User;
import com.voucherpro.model.UserRole;
import com.voucherpro.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        User user = new User(
                email,
                request.getName().trim(),
                passwordEncoder.encode(request.getPassword())
        );

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        return new AuthResponse(null, user.getName(), user.getEmail(), resolveRole(user).name());
    }

    private UserRole resolveRole(User user) {
        return user.getRole() != null ? user.getRole() : UserRole.USER;
    }

    private AuthResponse buildAuthResponse(User user) {
        UserRole role = user.getRole() != null ? user.getRole() : UserRole.USER;
        String token = jwtService.generateToken(user.getEmail(), user.getName(), role);
        return new AuthResponse(token, user.getName(), user.getEmail(), role.name());
    }
}
