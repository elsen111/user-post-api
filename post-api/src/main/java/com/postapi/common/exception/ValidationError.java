package com.postapi.common.exception;

import java.util.Map;

public record ValidationError(
        Map<String, String> fields
) {
}