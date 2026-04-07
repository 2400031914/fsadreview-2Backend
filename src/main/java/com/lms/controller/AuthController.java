package com.lms.controller;

import com.lms.dto.ApiMessageResponse;
import com.lms.dto.AuthResponse;
import com.lms.dto.OtpSendRequest;
import com.lms.dto.OtpVerifyRequest;
import com.lms.dto.RegisterRequest;
import com.lms.dto.UserResponse;
import com.lms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Registration, OTP login, and JWT user session APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Registers a user with username/email/password/role and triggers OTP to registered email.")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register payload username={}, email={}, role={}",
                request.getUsername(), request.getEmail(), request.getRole());
        UserResponse response = authService.register(request);
        log.info("POST /api/auth/register success username={}", response.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send OTP", description = "Validates username/password, generates OTP and emails it to the user's registered email.")
    public ResponseEntity<ApiMessageResponse> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    // Compatibility for existing frontend clients that still call /auth/login.
    @PostMapping("/login")
    @Operation(summary = "Login (send OTP)", description = "Alias of send-otp for compatibility with existing clients.")
    public ResponseEntity<ApiMessageResponse> login(@Valid @RequestBody OtpSendRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies OTP for username and returns JWT token + role.")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Returns current authenticated user details (requires JWT).")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }
}
