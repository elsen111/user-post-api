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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {

        User user = getAuthenticatedUser();

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(
            UpdateUserRequest request
    ) {

        User user = getAuthenticatedUser();

        String username = request.username().trim();
        String email = normalizeEmail(request.email());

        if (!user.getUsername().equalsIgnoreCase(username)
                && userRepository.existsByUsernameIgnoreCase(username)) {

            throw new ConflictException(
                    "Username is already in use"
            );
        }

        if (!user.getEmail().equalsIgnoreCase(email)
                && userRepository.existsByEmailIgnoreCase(email)) {

            throw new ConflictException(
                    "Email is already in use"
            );
        }

        user.setUsername(username);
        user.setEmail(email);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void deleteCurrentUser() {

        User user = getAuthenticatedUser();

        userRepository.delete(user);
    }

    private User getAuthenticatedUser() {

        UserPrincipal principal =
                currentUser.getPrincipal();

        UUID userId = principal.getId();

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private String normalizeEmail(String email) {
        return email.trim()
                .toLowerCase(Locale.ROOT);
    }
}