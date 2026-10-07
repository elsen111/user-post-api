package com.postapi.auth.service;

import com.postapi.auth.dto.AuthResponse;
import com.postapi.auth.dto.LoginRequest;
import com.postapi.auth.dto.RefreshTokenRequest;
import com.postapi.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);


}
