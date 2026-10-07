package com.postapi.comment.service;

import com.postapi.comment.dto.CommentResponse;
import com.postapi.comment.dto.CreateCommentRequest;
import com.postapi.comment.dto.UpdateCommentRequest;
import com.postapi.comment.entity.Comment;
import com.postapi.comment.mapper.CommentMapper;
import com.postapi.comment.repository.CommentRepository;
import com.postapi.common.dto.PageResponse;
import com.postapi.common.exception.ForbiddenException;
import com.postapi.common.exception.ResourceNotFoundException;
import com.postapi.common.security.CurrentUser;
import com.postapi.common.security.UserPrincipal;
import com.postapi.post.entity.Post;
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
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public CommentResponse createComment(
            UUID postId,
            CreateCommentRequest request
    ) {

        UserPrincipal principal =
                currentUser.getPrincipal();

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Post not found"
                        )
                );

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.content().trim())
                .build();

        commentRepository.save(comment);

        return commentMapper.toResponse(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getPostComments(
            UUID postId,
            Pageable pageable
    ) {

        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException(
                    "Post not found"
            );
        }

        Page<CommentResponse> page =
                commentRepository
                        .findAllByPostId(
                                postId,
                                pageable
                        )
                        .map(commentMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(
            UUID commentId,
            UpdateCommentRequest request
    ) {

        Comment comment =
                findComment(commentId);

        checkOwnership(comment);

        comment.setContent(
                request.content().trim()
        );

        return commentMapper.toResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {

        Comment comment =
                findComment(commentId);

        checkOwnership(comment);

        commentRepository.delete(comment);
    }

    private Comment findComment(UUID commentId) {

        return commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Comment not found"
                        )
                );
    }

    private void checkOwnership(Comment comment) {

        UUID currentUserId =
                currentUser.getPrincipal().getId();

        UUID ownerId =
                comment.getUser().getId();

        if (!ownerId.equals(currentUserId)) {

            throw new ForbiddenException(
                    "You do not have permission to modify this comment"
            );
        }
    }
}