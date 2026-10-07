package com.postapi.auth.dto;

import com.postapi.user.entity.UserRole;

import java.util.UUID;

public record AuthResponse(

        String tokenType,

        String accessToken,

        String refreshToken,

        long expiresIn,

        UserInfo user

) {

    public record UserInfo(

            UUID id,
            String username,
            String email,
            UserRole role

    ) {
    }
}