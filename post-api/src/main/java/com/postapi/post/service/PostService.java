package com.postapi.post.service;

import com.postapi.common.dto.PageResponse;
import com.postapi.post.dto.CreatePostRequest;
import com.postapi.post.dto.PostResponse;
import com.postapi.post.dto.UpdatePostRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PostService {

    PostResponse createPost(CreatePostRequest request);

    PageResponse<PostResponse> getPosts(Pageable pageable);

    PostResponse getPost(UUID postId);

    PageResponse<PostResponse> getUserPosts(
            UUID userId,
            Pageable pageable
    );

    PostResponse updatePost(
            UUID postId,
            UpdatePostRequest request
    );

    void deletePost(UUID postId);


}
