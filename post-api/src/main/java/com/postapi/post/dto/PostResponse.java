package com.postapi.post.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(

        UUID id,
        UUID userId,
        String username,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
