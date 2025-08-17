package com.marketplace.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.marketplace.auth.RegisterRequest;
import com.marketplace.exception.APIException;
import com.marketplace.models.User;
import com.marketplace.repository.UserRepository;
import com.marketplace.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    private final UserRepository userRepo;
    @Autowired
    private JwtUtil jwtUtil;

    private String generateVerificationCode() {
        int code = 100000 + new java.util.Random().nextInt(900000);
        return String.valueOf(code);
    }

    public String register(RegisterRequest request) {
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new APIException("User already exists", HttpStatus.CONFLICT);
        }

        User user = new User();
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(hashedPassword);
        user.setVerified(false);
        String verificationCode = generateVerificationCode();
        user.setVerificationToken(verificationCode);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepo.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
        String token = jwtUtil.generateToken(user.getEmail());
        userRepo.save(user);
        return token;
    }

    public String login(String emailString, String password) {
        User user = userRepo.findByEmail(emailString)
                .orElseThrow(() -> new APIException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isVerified()) {
            throw new APIException("Please verify your email before logging in.", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new APIException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return token;
    }

    public ResponseEntity<?> verifyEmail(String authHeader, Map<String, String> requestBody) {
        String jwtToken = authHeader.replace("Bearer ", "");

        String email = jwtUtil.extractUsername(jwtToken);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }

        String verificationCode = requestBody.get("verificationCode");
        if (verificationCode == null || verificationCode.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Verification code is required"));
        }

        // Find user from DB
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new APIException("User not found", HttpStatus.NOT_FOUND));

        // Already verified check
        if (user.isVerified()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "User already verified"));
        }

        // Token match check
        if (!verificationCode.equals(user.getVerificationToken())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid verification code"));
        }

        // Expiry check
        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Verification code expired"));
        }

        // Mark as verified
        user.setVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepo.save(user);

        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    public String resendVerificationCode(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new APIException("User not found", HttpStatus.NOT_FOUND));

        if (user.isVerified()) {
            throw new APIException("User already verified", HttpStatus.CONFLICT);
        }

        String verificationCode = generateVerificationCode();
        user.setVerificationToken(verificationCode);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepo.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        return "Verification code resent successfully";
    }

}
