package com.example.UserService.mapper;

import com.example.UserService.dto.user.UserByIdResponse;
import com.example.UserService.dto.user.UserRequest;
import com.example.UserService.dto.user.UserResponse;
import com.example.UserService.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CardMapper.class)
public interface UserMapper {
    @Mapping(target = "cards", ignore = true)
    User toEntity(UserRequest request);

    UserResponse toResponse(User user);

    UserByIdResponse toByIdResponse(User user);

    @Mapping(target = "cards", ignore = true)
    void updateUserFromRequest(UserRequest request, @MappingTarget User user);

}
