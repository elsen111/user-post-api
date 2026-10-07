package com.postapi.post.dto;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(

        UUID id,
        UUID userId,
        String username,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt

) {
}
