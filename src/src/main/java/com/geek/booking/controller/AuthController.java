package com.geek.booking.controller;

import com.geek.booking.dto.request.user.LoginRequest;
import com.geek.booking.dto.request.user.RefreshTokenRequest;
import com.geek.booking.dto.request.user.RegisterRequest;
import com.geek.booking.dto.response.JwtResponse;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public authentication endpoints (no token required).")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password", description = "Returns access and refresh tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful, returns JWT tokens"),
            @ApiResponse(responseCode = "400", description = "Invalid email/password or missing fields"),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account", description = "Creates a user with CUSTOMER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful, returns user details"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already exists")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Use a valid refresh token to obtain a new access token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New access token issued"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }
}