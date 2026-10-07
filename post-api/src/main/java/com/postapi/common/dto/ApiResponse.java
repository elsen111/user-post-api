package com.postapi.common.dto;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return new ApiResponse<>(
                true,
                message,
                data,
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Request successful", data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(
                true,
                message,
                null,
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> error(
            String message,
            T data
    ) {
        return new ApiResponse<>(
                false,
                message,
                data,
                Instant.now()
        );
    }

    public static ApiResponse<Void> error(String message) {
        return error(message, null);
    }
}