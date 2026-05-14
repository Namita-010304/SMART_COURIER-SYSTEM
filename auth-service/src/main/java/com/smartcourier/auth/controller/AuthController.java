package com.smartcourier.auth.controller;

import com.smartcourier.auth.dto.*;
import com.smartcourier.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(request));
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserResponseDTO> getMe(@RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(authService.getUserByUsername(username));
    }

    @PutMapping("/users/me")
    public ResponseEntity<UserResponseDTO> updateMe(
            @RequestHeader("X-User-Username") String username,
            @RequestBody UserUpdateDTO request) {
        // Prevent self-role-escalation
        request.setRole(null);
        return ResponseEntity.ok(authService.updateUserByUsername(username, request));
    }

    @PostMapping("/users/me/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Username") String username,
            @RequestBody ChangePasswordRequest request) {
        authService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<java.util.List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO request) {
        return ResponseEntity.ok(authService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
