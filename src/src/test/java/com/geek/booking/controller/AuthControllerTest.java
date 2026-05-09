package com.geek.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.geek.booking.dto.request.user.LoginRequest;
import com.geek.booking.dto.request.user.RegisterRequest;
import com.geek.booking.dto.response.JwtResponse;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.enums.UserRole;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/auth/register - should register new user")
    void register_ShouldReturnUserResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setName("New User");
        request.setPassword("password123");

        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("new@example.com")
                .name("New User")
                .role(UserRole.CUSTOMER)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login - should return JWT tokens")
    void login_ShouldReturnTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@geek.com");
        request.setPassword("admin123");

        JwtResponse response = JwtResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"));
    }
}