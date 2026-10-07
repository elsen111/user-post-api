package com.postapi.auth.service;

import com.postapi.auth.dto.AuthResponse;
import com.postapi.auth.dto.LoginRequest;
import com.postapi.auth.dto.RefreshTokenRequest;
import com.postapi.auth.dto.RegisterRequest;
import com.postapi.auth.entity.RefreshToken;
import com.postapi.auth.repository.RefreshTokenRepository;
import com.postapi.common.security.JwtProperties;
import com.postapi.common.security.JwtService;
import com.postapi.common.security.TokenHashUtil;
import com.postapi.common.security.UserPrincipal;
import com.postapi.user.entity.User;
import com.postapi.user.entity.UserRole;
import com.postapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenGenerator;
    private final TokenHashUtil tokenHashUtil;
    private final JwtProperties jwtProperties;

    @Transactional
    @Override
    public AuthResponse register(RegisterRequest request) {

        String username = request.username().trim();
        String email = normalizeEmail(request.email());

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException(
                    "Username is already in use"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email is already in use"
            );
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        UserPrincipal principal = UserPrincipal.from(user);

        return createAuthResponse(principal, user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.password()
                        )
                );

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        return createAuthResponse(principal, user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {

        String tokenHash =
                tokenHashUtil.hash(request.refreshToken());

        RefreshToken currentToken =
                refreshTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid refresh token"
                                )
                        );

        if (!currentToken.isUsable()) {
            throw new IllegalArgumentException(
                    "Refresh token is expired or revoked"
            );
        }

        User user = currentToken.getUser();

        currentToken.revoke();

        UserPrincipal principal =
                UserPrincipal.from(user);

        return createAuthResponse(principal, user);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {

        String tokenHash =
                tokenHashUtil.hash(request.refreshToken());

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(RefreshToken::revoke);
    }

    private AuthResponse createAuthResponse(
            UserPrincipal principal,
            User user
    ) {

        String accessToken =
                jwtService.generateAccessToken(principal);

        String rawRefreshToken =
                refreshTokenGenerator.generate();

        String refreshTokenHash =
                tokenHashUtil.hash(rawRefreshToken);

        Instant expiresAt = Instant.now()
                .plusMillis(
                        jwtProperties.refreshTokenExpiration()
                );

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(refreshTokenHash)
                        .expiresAt(expiresAt)
                        .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                "Bearer",
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenExpiration() / 1000,
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole()
                )
        );
    }

    private String normalizeEmail(String email) {
        return email.trim()
                .toLowerCase(Locale.ROOT);
    }
}