package com.geek.booking.dto.request.user;


import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String password;
    private String role;
}