package com.example.UserService.security.service;

import com.example.UserService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSecurityService {

    private final UserRepository userRepository;

    public boolean isOwner(String email, Long userId) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId().equals(userId))
                .orElse(false);
    }
}
