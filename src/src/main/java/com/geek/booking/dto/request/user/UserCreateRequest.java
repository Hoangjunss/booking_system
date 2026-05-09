package com.geek.booking.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String name;
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    private String role; // CUSTOMER, OPERATOR, ADMIN
}