package com.postapi.auth.service;

import com.postapi.auth.dto.AuthResponse;
import com.postapi.auth.dto.LoginRequest;
import com.postapi.auth.dto.RefreshTokenRequest;
import com.postapi.auth.dto.RegisterRequest;
import com.postapi.auth.entity.RefreshToken;
import com.postapi.auth.repository.RefreshTokenRepository;
import com.postapi.common.exception.ConflictException;
import com.postapi.common.exception.InvalidTokenException;
import com.postapi.common.security.JwtProperties;
import com.postapi.common.security.JwtService;
import com.postapi.common.security.TokenHashUtil;
import com.postapi.common.security.UserPrincipal;
import com.postapi.user.entity.User;
import com.postapi.user.entity.UserRole;
import com.postapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenGenerator;

    @Mock
    private TokenHashUtil tokenHashUtil;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

        user = User.builder()
                .username("elshan")
                .email("elshan@example.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .enabled(true)
                .build();
        user.setId(userId);
    }

    /** Stubs everything createAuthResponse(...) needs. */
    private void stubTokenCreation() {

        when(jwtService.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn("access-token");

        when(jwtService.getAccessTokenExpiration())
                .thenReturn(900_000L);

        when(jwtProperties.refreshTokenExpiration())
                .thenReturn(604_800_000L);

        when(refreshTokenGenerator.generate())
                .thenReturn("new-refresh-token");

        when(tokenHashUtil.hash("new-refresh-token"))
                .thenReturn("new-refresh-token-hash");
    }

    private RefreshToken tokenOf(boolean revoked, Instant expiresAt) {
        return RefreshToken.builder()
                .user(user)
                .tokenHash("hashed-token")
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
    }

    @Test
    void register_shouldCreateUserAndTokens() {

        RegisterRequest request = new RegisterRequest(
                "elshan",
                "elshan@example.com",
                "password123"
        );

        when(userRepository.existsByUsernameIgnoreCase("elshan"))
                .thenReturn(false);

        when(userRepository.existsByEmailIgnoreCase("elshan@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        stubTokenCreation();

        AuthResponse response = authService.register(request);

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().email()).isEqualTo("elshan@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword())
                .isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getRole())
                .isEqualTo(UserRole.USER);

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isUsable()).isTrue();
    }

    @Test
    void register_shouldRejectDuplicateUsername() {

        RegisterRequest request = new RegisterRequest(
                "existing",
                "elshan@example.com",
                "password123"
        );

        when(userRepository.existsByUsernameIgnoreCase("existing"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest(
                "elshan",
                "existing@example.com",
                "password123"
        );

        when(userRepository.existsByUsernameIgnoreCase("elshan"))
                .thenReturn(false);

        when(userRepository.existsByEmailIgnoreCase("existing@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void login_shouldReturnTokens() {

        LoginRequest request = new LoginRequest(
                "elshan@example.com",
                "password123"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(userPrincipal);

        when(userPrincipal.getId())
                .thenReturn(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        stubTokenCreation();

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.user().id()).isEqualTo(userId);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldRotateValidRefreshToken() {

        RefreshTokenRequest request =
                new RefreshTokenRequest("old-refresh-token");

        RefreshToken oldToken =
                tokenOf(false, Instant.now().plusSeconds(3600));

        when(tokenHashUtil.hash("old-refresh-token"))
                .thenReturn("hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(oldToken));

        stubTokenCreation();

        AuthResponse response = authService.refresh(request);

        assertThat(oldToken.isUsable()).isFalse();
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.accessToken()).isEqualTo("access-token");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldRejectUnknownRefreshToken() {

        RefreshTokenRequest request =
                new RefreshTokenRequest("invalid-refresh-token");

        when(tokenHashUtil.hash("invalid-refresh-token"))
                .thenReturn("hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldRejectRevokedRefreshToken() {

        RefreshTokenRequest request =
                new RefreshTokenRequest("refresh-token");

        RefreshToken token =
                tokenOf(true, Instant.now().plusSeconds(3600));

        when(tokenHashUtil.hash("refresh-token"))
                .thenReturn("hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldRejectExpiredRefreshToken() {

        RefreshTokenRequest request =
                new RefreshTokenRequest("refresh-token");

        RefreshToken token =
                tokenOf(false, Instant.now().minusSeconds(3600));

        when(tokenHashUtil.hash("refresh-token"))
                .thenReturn("hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logout_shouldRevokeExistingRefreshToken() {

        RefreshTokenRequest request =
                new RefreshTokenRequest("refresh-token");

        RefreshToken token =
                tokenOf(false, Instant.now().plusSeconds(3600));

        when(tokenHashUtil.hash("refresh-token"))
                .thenReturn("hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(token));

        authService.logout(request);

        assertThat(token.isUsable()).isFalse();
    }

    @Test
    void logout_shouldDoNothingWhenTokenIsUnknown() {

        RefreshTokenRequest request =
                new RefreshTokenRequest("unknown-token");

        when(tokenHashUtil.hash("unknown-token"))
                .thenReturn("hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.empty());

        authService.logout(request);

        verify(refreshTokenRepository).findByTokenHash("hashed-token");
        verifyNoMoreInteractions(refreshTokenRepository);
    }
}