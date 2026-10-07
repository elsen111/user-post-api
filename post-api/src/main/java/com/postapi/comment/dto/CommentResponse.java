package com.postapi.comment.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(

        UUID id,
        UUID postId,
        UUID userId,
        String username,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
