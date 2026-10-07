package com.postapi.admin.service;

import com.postapi.common.dto.PageResponse;
import com.postapi.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminService {

    PageResponse<UserResponse> getUsers(Pageable pageable);

    void deletePost(UUID postId);

    void deleteComment(UUID commentId);

}
