package com.postapi.admin.service;

import com.postapi.comment.entity.Comment;
import com.postapi.comment.repository.CommentRepository;
import com.postapi.common.exception.ResourceNotFoundException;
import com.postapi.post.entity.Post;
import com.postapi.post.repository.PostRepository;
import com.postapi.user.entity.User;
import com.postapi.user.repository.UserRepository;
import com.postapi.user.mapper.UserMapper;
import com.postapi.user.dto.UserResponse;
import com.postapi.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> getUsers(Pageable pageable) {

        Page<User> users = userRepository.findAll(pageable);

        Page<UserResponse> response = users.map(userMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Post not found: " + postId
                        )
                );

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Comment not found: " + commentId
                        )
                );

        commentRepository.delete(comment);
    }
}