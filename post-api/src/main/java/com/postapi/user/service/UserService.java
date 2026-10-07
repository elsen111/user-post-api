package com.postapi.user.service;

import com.postapi.user.dto.UpdateUserRequest;
import com.postapi.user.dto.UserResponse;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse updateCurrentUser(UpdateUserRequest request);

    void deleteCurrentUser();
}
