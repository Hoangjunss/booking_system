package com.geek.booking.service.impl;


import com.geek.booking.dto.request.user.UserCreateRequest;
import com.geek.booking.dto.request.user.UserUpdateRequest;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.entity.User;
import com.geek.booking.enums.UserRole;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.UserMapper;
import com.geek.booking.repository.UserRepository;
import com.geek.booking.service.UserService;
import com.geek.booking.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        User saved = userRepository.save(user);
        log.info("User created: id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = getEntityById(id);
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        }
        User updated = userRepository.save(user);
        return userMapper.toResponse(updated);
    }

    @Override
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(getEntityById(id));
    }

    @Override
    public Page<UserResponse> getAllUsers(String email, String name, String role, Pageable pageable) {
        UserRole userRole = role != null && !role.isBlank() ? UserRole.valueOf(role.toUpperCase()) : null;
        return userRepository.findAll(UserSpecification.filterBy(email, name, userRole), pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public boolean isAdmin(Long userId) {
        User user = getEntityById(userId);
        return user.getRole() == UserRole.ADMIN;
    }
}