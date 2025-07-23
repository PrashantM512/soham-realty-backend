package com.soham.realty.service;

import com.soham.realty.dto.request.LoginRequest;
import com.soham.realty.dto.request.RegisterRequest;
import com.soham.realty.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse register(RegisterRequest registerRequest);
}
