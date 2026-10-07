package com.postapi.post;

import com.postapi.common.exception.ForbiddenException;
import com.postapi.common.exception.ResourceNotFoundException;
import com.postapi.common.security.CurrentUser;
import com.postapi.common.security.UserPrincipal;
import com.postapi.post.dto.CreatePostRequest;
import com.postapi.post.dto.PostResponse;
import com.postapi.post.entity.Post;
import com.postapi.post.mapper.PostMapper;
import com.postapi.post.repository.PostRepository;
import com.postapi.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private PostService postService;

    private UUID userId;
    private User user;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .username("elshan")
                .email("elshan@example.com")
                .password("password")
                .build();

        principal = mock(UserPrincipal.class);

        when(principal.getId()).thenReturn(userId);
        when(currentUser.getPrincipal()).thenReturn(principal);
    }

    @Test
    void createPost_shouldCreatePostForCurrentUser() {

        CreatePostRequest request =
                new CreatePostRequest(
                        "My first post",
                        "Post content"
                );

        Post post = Post.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();

        PostResponse response = new PostResponse(
                UUID.randomUUID(),
                userId,
                "elshan",
                "My first post",
                "Post content",
                null,
                null
        );

        when(postRepository.save(any(Post.class)))
                .thenReturn(post);

        when(postMapper.toResponse(post))
                .thenReturn(response);

        PostResponse result =
                postService.createPost(request);

        assertThat(result).isEqualTo(response);

        verify(postRepository).save(any(Post.class));
        verify(postMapper).toResponse(post);
    }
}