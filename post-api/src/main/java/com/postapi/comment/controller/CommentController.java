package com.postapi.comment.controller;

import com.postapi.comment.dto.CommentResponse;
import com.postapi.comment.dto.CreateCommentRequest;
import com.postapi.comment.dto.UpdateCommentRequest;
import com.postapi.comment.service.CommentService;
import com.postapi.common.dto.ApiResponse;
import com.postapi.common.dto.PageResponse;
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
@RequiredArgsConstructor
@Tag(name = "Comment Management", description = "Endpoints for creating, reading, updating, and deleting post comments")
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "Create a comment on a post",
            description = "Adds a brand new comment to a specific blog or user post using the post's unique UUID identifier."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Comment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post target database entity not found")
    })
    @PostMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {

        CommentResponse response =
                commentService.createComment(
                        postId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Comment created successfully",
                                response
                        )
                );
    }

    @Operation(
            summary = "Get paginated comments for a post",
            description = "Retrieves a scrollable, split page response containing all comment histories left on a single designated post."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Target post matching the provided ID does not exist")
    })
    @GetMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<
            ApiResponse<PageResponse<CommentResponse>>
            > getPostComments(
            @PathVariable UUID postId,
            Pageable pageable
    ) {

        PageResponse<CommentResponse> response =
                commentService.getPostComments(
                        postId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Comments retrieved successfully",
                        response
                )
        );
    }

    @Operation(
            summary = "Partially update a comment text body",
            description = "Modifies text contents of an existing comment entry record. Patches only specified parameter updates."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request attributes payload text body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Unauthorized modification block context (Not your comment to edit)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No existing comment matching this ID was discovered")
    })
    @PatchMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {

        CommentResponse response =
                commentService.updateComment(
                        commentId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Comment updated successfully",
                        response
                )
        );
    }


    @Operation(
            summary = "Remove or delete a comment profile",
            description = "Permanently deletes a matching comment out of database storage tracking tables entirely using its UUID index key."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient access permissions to complete the removal execution"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment requested for destruction is not present")
    })
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId
    ) {

        commentService.deleteComment(commentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Comment deleted successfully"
                )
        );
    }
}