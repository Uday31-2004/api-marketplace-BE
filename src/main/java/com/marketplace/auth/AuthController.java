package com.marketplace.auth;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.marketplace.utils.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private JwtUtil jwtUtil;
    private UserRepository userRepository;

    public AuthController(AuthService authService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> requestBody) {
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @GetMapping("/ping")
    public String ping() {
        return "API WORKING";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Please verify your email before logging in.");
        }

        // Password check...
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(token);
    }

}
