package com.postapi.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(

        UUID id,
        UUID postId,
        UUID userId,
        String username,
        String content,
        Instant createdAt,
        Instant updatedAt

) {
}
