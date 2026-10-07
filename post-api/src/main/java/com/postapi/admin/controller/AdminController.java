package com.postapi.admin.controller;

import com.postapi.admin.service.AdminService;
import com.postapi.common.dto.ApiResponse;
import com.postapi.common.dto.PageResponse;
import com.postapi.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Elevated administrative endpoints for platform moderation, user auditing, and content removal override controls")
public class AdminController {

    private final AdminService adminService;

    @Operation(
            summary = "Fetch a paginated list of all system users",
            description = "Allows an administrator to audit all user accounts registered across the entire system platform."
    )
    @ApiResponses(
            {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users fetched successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or expired authorization credentials"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: Insufficient administrative privileges")
            }
    )
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            Pageable pageable
    ) {

        PageResponse<UserResponse> response = adminService.getUsers(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "Users fetched sucessfully",
                response
        ));

    }

    @Operation(
            summary = "Administrative post deletion override",
            description = "Forces the permanent removal of any user post from the system database regardless of original author ownership (Moderation utility)."
    )
    @ApiResponses(
            {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or malformed authorization token context"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: Account lacks required administrative role privileges"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "The requested post target was not found")
            }
    )
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId
    ) {

        adminService.deletePost(postId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Post deleted successfully"
                )
        );
    }

    @Operation(
            summary = "Administrative comment deletion override",
            description = "Forces the permanent removal of any comment entry out of platform storage tracking tables for moderation purposes."
    )
    @ApiResponses(
            {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment deleted successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing session authorization validation profiles"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied: Operational authority restricted to administrators"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment requested for administrative destruction is missing")
            }
    )
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId
    ) {

        adminService.deleteComment(commentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Comment deleted successfully"
                )
        );
    }

}
