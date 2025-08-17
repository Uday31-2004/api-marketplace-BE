package com.marketplace.auth;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketplace.repository.UserRepository;
import com.marketplace.service.AuthService;
import com.marketplace.utils.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authService = authService;

    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest user) {
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationCode(@RequestBody String email) {
        return ResponseEntity.ok(authService.resendVerificationCode(email));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> requestBody) {

        return authService.verifyEmail(authHeader, requestBody);
    }

    @GetMapping("/ping")
    public String ping() {
        return "API WORKING";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req.getEmail(), req.getPassword()));
    }

}
