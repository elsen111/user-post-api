package com.postapi.post.service;

import com.postapi.common.dto.PageResponse;
import com.postapi.common.exception.ForbiddenException;
import com.postapi.common.exception.ResourceNotFoundException;
import com.postapi.common.security.CurrentUser;
import com.postapi.common.security.UserPrincipal;
import com.postapi.post.dto.CreatePostRequest;
import com.postapi.post.dto.PostResponse;
import com.postapi.post.dto.UpdatePostRequest;
import com.postapi.post.entity.Post;
import com.postapi.post.mapper.PostMapper;
import com.postapi.post.repository.PostRepository;
import com.postapi.user.entity.User;
import com.postapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public PostResponse createPost(
            CreatePostRequest request
    ) {

        UserPrincipal principal =
                currentUser.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        Post post = Post.builder()
                .user(user)
                .title(request.title().trim())
                .content(request.content().trim())
                .build();

        postRepository.save(post);

        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPosts(
            Pageable pageable
    ) {

        Page<PostResponse> page =
                postRepository.findAll(pageable)
                        .map(postMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {

        Post post = findPost(postId);

        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(
            UUID userId,
            Pageable pageable
    ) {

        Page<PostResponse> page =
                postRepository
                        .findAllByUserId(userId, pageable)
                        .map(postMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public PostResponse updatePost(
            UUID postId,
            UpdatePostRequest request
    ) {

        Post post = findPost(postId);

        checkOwnership(post);

        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());

        return postMapper.toResponse(post);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {

        Post post = findPost(postId);

        checkOwnership(post);

        postRepository.delete(post);
    }

    private Post findPost(UUID postId) {

        return postRepository.findById(postId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Post not found"
                        )
                );
    }

    private void checkOwnership(Post post) {

        UUID currentUserId =
                currentUser.getPrincipal().getId();

        UUID ownerId =
                post.getUser().getId();

        if (!ownerId.equals(currentUserId)) {

            throw new ForbiddenException(
                    "You do not have permission to modify this post"
            );
        }
    }
}