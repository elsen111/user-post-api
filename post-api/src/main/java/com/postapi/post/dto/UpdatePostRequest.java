package com.postapi.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(

        @NotBlank(message = "Title is required")
        @Size(
                max = 150,
                message = "Title must not exceed 150 characters"
        )
        String title,

        @NotBlank(message = "Content is required")
        @Size(
                max = 10000,
                message = "Content must not exceed 10000 characters"
        )
        String content

) {
}