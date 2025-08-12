package com.marketplace.auth;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.marketplace.utils.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    @Autowired
    private JwtUtil jwtUtil;

    private String generateVerificationCode() {
        int code = 100000 + new java.util.Random().nextInt(900000);
        return String.valueOf(code);
    }

    public String register(User user) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }
        user.setVerified(false);
        user.setVerificationToken(generateVerificationCode());
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepo.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        userRepo.save(user);
        return token;
    }
}
