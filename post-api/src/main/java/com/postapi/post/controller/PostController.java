package com.postapi.post.controller;

import com.postapi.common.dto.ApiResponse;
import com.postapi.common.dto.PageResponse;
import com.postapi.post.dto.CreatePostRequest;
import com.postapi.post.dto.PostResponse;
import com.postapi.post.dto.UpdatePostRequest;
import com.postapi.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post Management", description = "Endpoints for creating, reading, modifying, and deleting user feed posts")
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "Create a new post",
            description = "Publishes a new post entry to the global platform feed."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Post created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid creation input attributes payload or validation failure"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or expired authorization credentials")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request
    ) {

        PostResponse response =
                postService.createPost(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Post created successfully",
                                response
                        )
                );
    }


    @Operation(
            summary = "Get all global posts",
            description = "Retrieves a scrollable, paginated record collection containing all user posts published on the system."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<PostResponse>>
            > getPosts(
            Pageable pageable
    ) {

        PageResponse<PostResponse> response =
                postService.getPosts(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Posts retrieved successfully",
                        response
                )
        );
    }

    @Operation(
            summary = "Fetch a post by its ID",
            description = "Locates and returns details for a single target post structure utilizing its unique system UUID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active post records matching this ID were discovered")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable UUID postId
    ) {

        PostResponse response =
                postService.getPost(postId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Post retrieved successfully",
                        response
                )
        );
    }

    @Operation(
            summary = "Get all posts written by a specific user",
            description = "Fetches a scrollable, split page index displaying every individual post created by a target author UUID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User posts retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Target writer user matching this ID does not exist")
    })
    @GetMapping("/user/{userId}")
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

    @Operation(
            summary = "Partially modify a post payload",
            description = "Updates individual properties (like text fields or titles) of an active post entry without replacing the entire layout resource."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid configuration data parameters body payload supplied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Action block exception (You do not own this post entry)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "The requested post target was not found")
    })
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {

        PostResponse response =
                postService.updatePost(
                        postId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Post updated successfully",
                        response
                )
        );
    }

    @Operation(
            summary = "Permanently remove a post entry",
            description = "Deletes a designated post completely out of the persistent storage infrastructure records by its unique ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privilege levels or ownership rules validation block to carry out erasure"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Target post resource intended for deletion is missing")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId
    ) {

        postService.deletePost(postId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Post deleted successfully"
                )
        );
    }
}