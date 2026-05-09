package com.geek.booking.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.booking.dto.request.user.UserCreateRequest;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.enums.UserRole;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/admin/users - should create user")
    void createUser_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("newadmin@geek.com");
        request.setName("Admin");
        request.setPassword("admin123");
        request.setRole("ADMIN");

        UserResponse response = UserResponse.builder().id(1L).email("newadmin@geek.com").name("Admin").role(UserRole.ADMIN).build();
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newadmin@geek.com"));
    }

    @Test
    @DisplayName("GET /api/admin/users - should return page of users")
    void getAllUsers_ShouldReturnPage() throws Exception {
        UserResponse response = UserResponse.builder().id(1L).email("user@example.com").build();
        Page<UserResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@example.com"));
    }
}