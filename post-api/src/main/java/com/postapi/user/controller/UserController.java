package com.postapi.user.controller;

import com.postapi.common.dto.ApiResponse;
import com.postapi.common.dto.PageResponse;
import com.postapi.post.dto.PostResponse;
import com.postapi.post.service.PostService;
import com.postapi.user.dto.UpdateUserRequest;
import com.postapi.user.dto.UserResponse;
import com.postapi.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user profiles, updating personal account details, and fetching author-specific content")
public class UserController {

    private final UserService userService;
    private final PostService postService;

    @Operation(
            summary = "Get the authenticated user session profile",
            description = "Resolves the identity of the current logged-in user from the security context via their JWT token and returns their profile details."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, malformed, or expired bearer token authentication header")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        UserResponse response =
                userService.getCurrentUser();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User retrieved successfully",
                        response
                )
        );
    }

    @Operation(
            summary = "Partially update the current user's profile details",
            description = "Modifies customizable configuration preferences or user metadata fields for the currently logged-in account identity context."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failed or unexpected parameter attributes supplied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access attempt block context")
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UserResponse response =
                userService.updateCurrentUser(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User updated successfully",
                        response
                )
        );
    }

    @Operation(
            summary = "Permanently close and delete the current user account",
            description = "Erases the authenticated operator's user account identity profile record entirely out of the active database schema."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Session authorization validation failure check block")
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser() {

        userService.deleteCurrentUser();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User deleted successfully"
                )
        );
    }

    @Operation(
            summary = "Fetch posts published by an individual user target",
            description = "Looks up a clean, scrollable page payload list compiling all public feed post elements created by a specific target author identity ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User posts retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No existing account records matching this requested user ID were discovered")
    })
    @GetMapping("/{userId}/posts")
    public ResponseEntity<
            ApiResponse<PageResponse<PostResponse>>
            > getUserPosts(
            @PathVariable UUID userId,
            Pageable pageable
    ) {

        PageResponse<PostResponse> response =
                postService.getUserPosts(
                        userId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User posts retrieved successfully",
                        response
                )
        );
    }
}