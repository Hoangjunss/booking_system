package com.geek.booking.service;


import com.geek.booking.dto.request.user.LoginRequest;
import com.geek.booking.dto.request.user.RefreshTokenRequest;
import com.geek.booking.dto.request.user.RegisterRequest;
import com.geek.booking.dto.response.JwtResponse;
import com.geek.booking.dto.response.UserResponse;

public interface AuthService {
    JwtResponse login(LoginRequest request);
    JwtResponse refreshAccessToken(RefreshTokenRequest request);
    UserResponse register(RegisterRequest request);
}