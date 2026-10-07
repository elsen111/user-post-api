package com.postapi.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        long capacity,
        long refillTokens,
        long refillDurationSeconds,
        long authCapacity,
        long authRefillTokens,
        long authRefillDurationSeconds
) {
}