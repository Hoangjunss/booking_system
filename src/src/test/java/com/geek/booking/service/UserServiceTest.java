package com.geek.booking.service;

import com.geek.booking.dto.request.user.UserCreateRequest;
import com.geek.booking.dto.request.user.UserUpdateRequest;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.entity.User;
import com.geek.booking.enums.UserRole;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.UserMapper;
import com.geek.booking.repository.UserRepository;
import com.geek.booking.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponse userResponse;
    private UserCreateRequest createRequest;
    private UserUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("john@example.com")
                .name("John Doe")
                .password("encodedPass")
                .role(UserRole.CUSTOMER)
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .email("john@example.com")
                .name("John Doe")
                .role(UserRole.CUSTOMER)
                .build();

        createRequest = new UserCreateRequest();
        createRequest.setEmail("new@example.com");
        createRequest.setName("New User");
        createRequest.setPassword("password123");
        createRequest.setRole("ADMIN");

        updateRequest = new UserUpdateRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setRole("OPERATOR");
    }


    @Test
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUser(1L, updateRequest);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(user.getName()).isEqualTo("Updated Name");
    }

    @Test
    void updateUser_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        UserResponse result = userService.getUserById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllUsers_WithFilters_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers("john", "John", "CUSTOMER", pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void isAdmin_WithAdmin_ReturnsTrue() {
        User adminUser = User.builder().id(1L).role(UserRole.ADMIN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        assertThat(userService.isAdmin(1L)).isTrue();
    }

    @Test
    void isAdmin_WithCustomer_ReturnsFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThat(userService.isAdmin(1L)).isFalse();
    }
}