package com.postapi.user.mapper;

import com.postapi.user.dto.UserResponse;
import com.postapi.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

}
