package com.postapi.user.dto;

import com.postapi.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(

        UUID id,
        String username,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt

) {
}