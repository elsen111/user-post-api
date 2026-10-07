package com.postapi.auth.service;

import com.postapi.auth.dto.AuthResponse;
import com.postapi.auth.dto.LoginRequest;
import com.postapi.auth.dto.RefreshTokenRequest;
import com.postapi.auth.dto.RegisterRequest;

public interface AuthService {

    public AuthResponse register(RegisterRequest request);

    public AuthResponse login(LoginRequest request);

    public AuthResponse refresh(RefreshTokenRequest request);

    public void logout(RefreshTokenRequest request);


}
