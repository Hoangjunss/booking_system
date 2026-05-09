package com.geek.booking.service.impl;

import com.geek.booking.dto.request.user.LoginRequest;
import com.geek.booking.dto.request.user.RefreshTokenRequest;
import com.geek.booking.dto.request.user.RegisterRequest;
import com.geek.booking.dto.response.JwtResponse;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.entity.User;
import com.geek.booking.enums.UserRole;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.mapper.UserMapper;
import com.geek.booking.repository.UserRepository;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.security.UserPrincipal;
import com.geek.booking.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public JwtResponse login(LoginRequest request) {
        // Xác thực email và password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(request.getEmail());

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getUsername());

        log.info("User logged in: {}", userPrincipal.getEmail());
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public JwtResponse refreshAccessToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }
        String email = tokenProvider.getEmailFromToken(refreshToken);
        UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(email);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .build();
        user = userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());
        return userMapper.toResponse(user);
    }
}