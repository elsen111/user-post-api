package com.postapi.post.service;

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
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private PostServiceImpl postService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .username("elshan")
                .email("elshan@example.com")
                .password("encoded-password")
                .build();
        user.setId(userId);

        lenient().when(currentUser.getPrincipal()).thenReturn(userPrincipal);
        lenient().when(userPrincipal.getId()).thenReturn(userId);
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

    private Post postOf(User owner, String title, String content) {
        return Post.builder()
                .user(owner)
                .title(title)
                .content(content)
                .build();
    }

    @Test
    void createPost_shouldCreatePostForCurrentUser() {

        CreatePostRequest request = new CreatePostRequest(
                "  My first post  ",
                "  This is my first post.  "
        );

        PostResponse response = new PostResponse(
                UUID.randomUUID(),
                userId,
                "elshan",
                "My first post",
                "This is my first post.",
                null,
                null
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(postMapper.toResponse(any(Post.class)))
                .thenReturn(response);

        PostResponse result = postService.createPost(request);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());

        Post saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTitle()).isEqualTo("My first post");
        assertThat(saved.getContent()).isEqualTo("This is my first post.");
    }

    @Test
    void getPost_shouldReturnPostWhenExists() {

        UUID postId = UUID.randomUUID();
        Post post = postOf(user, "Test post", "Test content");

        PostResponse response = new PostResponse(
                postId,
                userId,
                "elshan",
                "Test post",
                "Test content",
                null,
                null
        );

        when(postRepository.findById(postId))
                .thenReturn(Optional.of(post));

        when(postMapper.toResponse(post))
                .thenReturn(response);

        PostResponse result = postService.getPost(postId);

        assertThat(result).isEqualTo(response);

        verify(postRepository).findById(postId);
        verify(postMapper).toResponse(post);
    }

    @Test
    void getPost_shouldThrowWhenPostDoesNotExist() {

        UUID postId = UUID.randomUUID();

        when(postRepository.findById(postId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(postId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository).findById(postId);
        verifyNoInteractions(postMapper);
    }

    @Test
    void updatePost_shouldUpdatePostOwnedByCurrentUser() {

        UUID postId = UUID.randomUUID();
        Post post = postOf(user, "Old title", "Old content");

        UpdatePostRequest request = new UpdatePostRequest(
                "New title",
                "New content"
        );

        PostResponse response = new PostResponse(
                postId,
                userId,
                "elshan",
                "New title",
                "New content",
                null,
                null
        );

        when(postRepository.findById(postId))
                .thenReturn(Optional.of(post));

        when(postMapper.toResponse(post))
                .thenReturn(response);

        PostResponse result = postService.updatePost(postId, request);

        assertThat(post.getTitle()).isEqualTo("New title");
        assertThat(post.getContent()).isEqualTo("New content");
        assertThat(result).isEqualTo(response);

        verify(postMapper).toResponse(post);
    }

    @Test
    void updatePost_shouldRejectPostOwnedByAnotherUser() {

        UUID postId = UUID.randomUUID();
        Post post = postOf(otherUser(), "Original title", "Original content");

        when(postRepository.findById(postId))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(() ->
                postService.updatePost(
                        postId,
                        new UpdatePostRequest("Updated title", "Updated content")
                )
        ).isInstanceOf(ForbiddenException.class);

        assertThat(post.getTitle()).isEqualTo("Original title");
        assertThat(post.getContent()).isEqualTo("Original content");

        verifyNoInteractions(postMapper);
    }

    @Test
    void deletePost_shouldDeletePostOwnedByCurrentUser() {

        UUID postId = UUID.randomUUID();
        Post post = postOf(user, "Test post", "Test content");

        when(postRepository.findById(postId))
                .thenReturn(Optional.of(post));

        postService.deletePost(postId);

        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_shouldRejectPostOwnedByAnotherUser() {

        UUID postId = UUID.randomUUID();
        Post post = postOf(otherUser(), "Test post", "Test content");

        when(postRepository.findById(postId))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(postId))
                .isInstanceOf(ForbiddenException.class);

        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    void deletePost_shouldThrowWhenPostDoesNotExist() {

        UUID postId = UUID.randomUUID();

        when(postRepository.findById(postId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(postId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).delete(any(Post.class));
    }
}