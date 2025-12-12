package com.mvt.mvt_events.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mvt.mvt_events.repository.UserRepository;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tenta buscar por email primeiro
        Optional<com.mvt.mvt_events.jpa.User> user = userRepository.findByUsernameForAuth(username);
        
        // Se não encontrou por email, tenta buscar por CPF
        if (user.isEmpty()) {
            user = userRepository.findByDocumentNumberForAuth(username);
        }
        
        return user.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}