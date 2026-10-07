package com.postapi.common.ratelimit;

import com.postapi.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import io.github.bucket4j.Bucket;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    private final Map<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    public RateLimitFilter(
            RateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!properties.enabled()
                || shouldSkip(request)) {

            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = buildClientKey(request);

        Bucket bucket = buckets.computeIfAbsent(
                clientKey,
                key -> createBucket(request)
        );

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        sendRateLimitResponse(response);
    }

    private Bucket createBucket(HttpServletRequest request) {

        if (isAuthenticationEndpoint(request)) {
            return Bucket.builder()
                    .addLimit(limit -> limit
                            .capacity(properties.authCapacity())
                            .refillGreedy(
                                    properties.authRefillTokens(),
                                    Duration.ofSeconds(
                                            properties.authRefillDurationSeconds()
                                    )
                            )
                    )
                    .build();
        }

        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(properties.capacity())
                        .refillGreedy(
                                properties.refillTokens(),
                                Duration.ofSeconds(
                                        properties.refillDurationSeconds()
                                )
                        )
                )
                .build();
    }

    private String buildClientKey(
            HttpServletRequest request
    ) {
        return request.getRemoteAddr()
                + ":"
                + (isAuthenticationEndpoint(request)
                ? "auth"
                : "api");
    }

    private boolean shouldSkip(
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        return !path.startsWith(API_PREFIX)
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/api/v3/api-docs")
                || path.equals("/api/swagger-ui.html");
    }

    private boolean isAuthenticationEndpoint(
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh");
    }

    private void sendRateLimitResponse(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        ApiResponse<Void> body = new ApiResponse<>(
                false,
                "Too many requests. Please try again later.",
                null,
                Instant.now()
        );

        response.getWriter()
                .write(objectMapper.writeValueAsString(body));
    }
}