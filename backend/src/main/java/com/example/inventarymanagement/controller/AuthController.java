package com.example.inventarymanagement.controller;

import com.example.inventarymanagement.config.CommonApiErrorResponses;
import com.example.inventarymanagement.dto.AuthLoginRequest;
import com.example.inventarymanagement.dto.AuthRegisterRequest;
import com.example.inventarymanagement.dto.AuthTokenResponse;
import com.example.inventarymanagement.dto.AuthUserResponse;
import com.example.inventarymanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and JWT authentication")
@CommonApiErrorResponses
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token", security = {})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful")
    })
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", security = {})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered")
    })
    public AuthUserResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }
}
