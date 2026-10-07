package com.postapi.user.service;

import com.postapi.common.exception.ConflictException;
import com.postapi.common.exception.ResourceNotFoundException;
import com.postapi.common.security.CurrentUser;
import com.postapi.common.security.UserPrincipal;
import com.postapi.user.dto.UpdateUserRequest;
import com.postapi.user.dto.UserResponse;
import com.postapi.user.entity.User;
import com.postapi.user.mapper.UserMapper;
import com.postapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private UserServiceImpl userService;

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

        lenient().when(currentUser.getPrincipal()).thenReturn(userPrincipal);
        lenient().when(userPrincipal.getId()).thenReturn(userId);
    }

    @Test
    void getCurrentUser_shouldReturnCurrentUser() {

        UserResponse response = new UserResponse(
                userId,
                "elshan",
                "elshan@example.com",
                user.getRole(),
                true,
                null,
                null
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result = userService.getCurrentUser();

        assertThat(result).isEqualTo(response);

        verify(userMapper).toResponse(user);
    }

    @Test
    void getCurrentUser_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(userMapper);
    }

    @Test
    void updateCurrentUser_shouldUpdateUser() {

        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                "newemail@example.com"
        );

        UserResponse response = new UserResponse(
                userId,
                "newusername",
                "newemail@example.com",
                user.getRole(),
                true,
                null,
                null
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameIgnoreCase("newusername"))
                .thenReturn(false);

        when(userRepository.existsByEmailIgnoreCase("newemail@example.com"))
                .thenReturn(false);

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result = userService.updateCurrentUser(request);

        assertThat(user.getUsername()).isEqualTo("newusername");
        assertThat(user.getEmail()).isEqualTo("newemail@example.com");
        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateCurrentUser_shouldRejectDuplicateUsername() {

        UpdateUserRequest request = new UpdateUserRequest(
                "existing",
                "newemail@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameIgnoreCase("existing"))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUser(request))
                .isInstanceOf(ConflictException.class);

        assertThat(user.getUsername()).isEqualTo("elshan");
        assertThat(user.getEmail()).isEqualTo("elshan@example.com");

        verifyNoInteractions(userMapper);
    }

    @Test
    void updateCurrentUser_shouldRejectDuplicateEmail() {

        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                "existing@example.com"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameIgnoreCase("newusername"))
                .thenReturn(false);

        when(userRepository.existsByEmailIgnoreCase("existing@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUser(request))
                .isInstanceOf(ConflictException.class);

        assertThat(user.getUsername()).isEqualTo("elshan");
        assertThat(user.getEmail()).isEqualTo("elshan@example.com");

        verifyNoInteractions(userMapper);
    }

    @Test
    void deleteCurrentUser_shouldDeleteCurrentUser() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        userService.deleteCurrentUser();

        verify(userRepository).delete(user);
    }

    @Test
    void deleteCurrentUser_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }
}