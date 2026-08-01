package com.example.inventarymanagement.security;

import com.example.inventarymanagement.entity.User;
import com.example.inventarymanagement.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String normalizedRole = user.getRole() == null ? "WAREHOUSE_STAFF" : user.getRole().toUpperCase(Locale.ROOT);
        String authority = normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getHashedPassword())
                .authorities(List.of(new SimpleGrantedAuthority(authority)))
                .accountLocked(false)
                .disabled(!Boolean.TRUE.equals(user.getActive()))
                .build();
    }
}
