package com.geek.booking.service;

import com.geek.booking.dto.request.user.UserCreateRequest;
import com.geek.booking.dto.request.user.UserUpdateRequest;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.Optional;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse getUserById(Long id);
    Page<UserResponse> getAllUsers(String email, String name, String role, Pageable pageable);
    User getEntityById(Long id);
    boolean isAdmin(Long userId);
}