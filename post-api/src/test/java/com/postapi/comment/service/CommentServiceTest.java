package com.postapi.comment.service;

import com.postapi.comment.dto.CommentResponse;
import com.postapi.comment.dto.CreateCommentRequest;
import com.postapi.comment.dto.UpdateCommentRequest;
import com.postapi.comment.entity.Comment;
import com.postapi.comment.mapper.CommentMapper;
import com.postapi.comment.repository.CommentRepository;
import com.postapi.common.exception.ForbiddenException;
import com.postapi.common.exception.ResourceNotFoundException;
import com.postapi.common.security.CurrentUser;
import com.postapi.common.security.UserPrincipal;
import com.postapi.post.entity.Post;
import com.postapi.post.repository.PostRepository;
import com.postapi.user.entity.User;
import com.postapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UUID userId;
    private User user;
    private Post post;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

        user = User.builder()
                .username("elshan")
                .email("elshan@example.com")
                .password("encoded-password")
                .build();
        user.setId(userId);

        post = Post.builder()
                .user(user)
                .title("Test post")
                .content("Test content")
                .build();

        lenient().when(currentUser.getPrincipal()).thenReturn(userPrincipal);
        lenient().when(userPrincipal.getId()).thenReturn(userId);
    }

    private Comment commentOf(User owner, String content) {
        return Comment.builder()
                .post(post)
                .user(owner)
                .content(content)
                .build();
    }

    private User otherUser() {
        User other = User.builder()
                .username("owner")
                .email("owner@example.com")
                .password("encoded-password")
                .build();
        other.setId(UUID.randomUUID());
        return other;
    }

    @Test
    void createComment_shouldCreateCommentForCurrentUser() {

        UUID postId = UUID.randomUUID();

        CreateCommentRequest request =
                new CreateCommentRequest("  Great post!  ");

        CommentResponse response = new CommentResponse(
                UUID.randomUUID(),
                postId,
                userId,
                "elshan",
                "Great post!",
                null,
                null
        );

        when(postRepository.findById(postId))
                .thenReturn(Optional.of(post));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(commentMapper.toResponse(any(Comment.class)))
                .thenReturn(response);

        CommentResponse result =
                commentService.createComment(postId, request);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Comment> captor =
                ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());

        Comment saved = captor.getValue();
        assertThat(saved.getPost()).isSameAs(post);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getContent()).isEqualTo("Great post!");
    }

    @Test
    void createComment_shouldThrowWhenPostDoesNotExist() {

        UUID postId = UUID.randomUUID();

        when(postRepository.findById(postId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                commentService.createComment(
                        postId,
                        new CreateCommentRequest("Comment")
                )
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_shouldUpdateOwnComment() {

        UUID commentId = UUID.randomUUID();
        Comment comment = commentOf(user, "Old comment");

        UpdateCommentRequest request =
                new UpdateCommentRequest("Updated comment");

        CommentResponse response = new CommentResponse(
                commentId,
                UUID.randomUUID(),
                userId,
                "elshan",
                "Updated comment",
                null,
                null
        );

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        when(commentMapper.toResponse(comment))
                .thenReturn(response);

        CommentResponse result =
                commentService.updateComment(commentId, request);

        assertThat(comment.getContent()).isEqualTo("Updated comment");
        assertThat(result).isEqualTo(response);

        verify(commentMapper).toResponse(comment);
    }

    @Test
    void updateComment_shouldRejectAnotherUsersComment() {

        UUID commentId = UUID.randomUUID();
        Comment comment = commentOf(otherUser(), "Original comment");

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                commentService.updateComment(
                        commentId,
                        new UpdateCommentRequest("Updated comment")
                )
        ).isInstanceOf(ForbiddenException.class);

        assertThat(comment.getContent()).isEqualTo("Original comment");

        verifyNoInteractions(commentMapper);
    }

    @Test
    void deleteComment_shouldDeleteOwnComment() {

        UUID commentId = UUID.randomUUID();
        Comment comment = commentOf(user, "My comment");

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        commentService.deleteComment(commentId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_shouldRejectAnotherUsersComment() {

        UUID commentId = UUID.randomUUID();
        Comment comment = commentOf(otherUser(), "Owner comment");

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(commentId))
                .isInstanceOf(ForbiddenException.class);

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void deleteComment_shouldThrowWhenCommentDoesNotExist() {

        UUID commentId = UUID.randomUUID();

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(commentId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).delete(any(Comment.class));
    }
}