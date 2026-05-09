package com.geek.booking.mapper;

import com.geek.booking.dto.request.user.UserCreateRequest;
import com.geek.booking.dto.response.UserResponse;
import com.geek.booking.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
    List<UserResponse> toResponseList(List<User> users);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", expression = "java(com.geek.booking.enums.UserRole.CUSTOMER)")
    User toEntity(UserCreateRequest request);
}