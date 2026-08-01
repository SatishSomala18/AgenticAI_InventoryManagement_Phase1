package com.example.inventarymanagement.service;

import com.example.inventarymanagement.dto.AuthLoginRequest;
import com.example.inventarymanagement.dto.AuthRegisterRequest;
import com.example.inventarymanagement.dto.AuthTokenResponse;
import com.example.inventarymanagement.dto.AuthUserResponse;

public interface AuthService {

    AuthTokenResponse login(AuthLoginRequest request);

    AuthUserResponse register(AuthRegisterRequest request);
}
