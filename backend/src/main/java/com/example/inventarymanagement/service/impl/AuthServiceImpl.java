package com.example.inventarymanagement.service.impl;

import com.example.inventarymanagement.dto.AuthLoginRequest;
import com.example.inventarymanagement.dto.AuthRegisterRequest;
import com.example.inventarymanagement.dto.AuthTokenResponse;
import com.example.inventarymanagement.dto.AuthUserResponse;
import com.example.inventarymanagement.entity.User;
import com.example.inventarymanagement.exception.BusinessValidationException;
import com.example.inventarymanagement.exception.DuplicateResourceException;
import com.example.inventarymanagement.repository.UserRepository;
import com.example.inventarymanagement.security.JwtTokenProvider;
import com.example.inventarymanagement.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResponse login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String authority = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_WAREHOUSE_STAFF");
        String role = authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority;

        AuthTokenResponse response = new AuthTokenResponse();
        response.setAccessToken(jwtTokenProvider.generateToken(userDetails, role));
        response.setExpiresInMs(jwtTokenProvider.getExpirationMs());
        response.setUsername(userDetails.getUsername());
        response.setRole(role);
        userRepository.findByEmail(userDetails.getUsername())
                .ifPresent(user -> response.setFullName(user.getFullName()));
        return response;
    }

    @Override
    public AuthUserResponse register(AuthRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateResourceException("User already exists with email: " + normalizedEmail);
        }

        String normalizedRole = request.getRole() == null
                ? "WAREHOUSE_STAFF"
                : request.getRole().trim().toUpperCase(Locale.ROOT);
        if (!"STORE_MANAGER".equals(normalizedRole)
                && !"INVENTORY_ANALYST".equals(normalizedRole)
                && !"PROCUREMENT_OFFICER".equals(normalizedRole)
                && !"WAREHOUSE_STAFF".equals(normalizedRole)) {
            throw new BusinessValidationException(
                    "Role must be STORE_MANAGER, INVENTORY_ANALYST, PROCUREMENT_OFFICER, or WAREHOUSE_STAFF");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName().trim());
        user.setRole(normalizedRole);
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private AuthUserResponse toResponse(User user) {
        AuthUserResponse response = new AuthUserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setActive(user.getActive());
        return response;
    }
}
