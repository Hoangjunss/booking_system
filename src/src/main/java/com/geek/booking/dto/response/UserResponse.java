package com.geek.booking.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import com.geek.booking.enums.UserRole;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private UserRole role;
    private LocalDateTime createdAt;
}