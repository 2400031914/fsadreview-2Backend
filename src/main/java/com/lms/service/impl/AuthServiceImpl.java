package com.lms.service.impl;

import com.lms.dto.ApiMessageResponse;
import com.lms.dto.AuthResponse;
import com.lms.dto.OtpSendRequest;
import com.lms.dto.OtpVerifyRequest;
import com.lms.dto.RegisterRequest;
import com.lms.dto.UserResponse;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.UnauthorizedException;
import com.lms.model.User;
import com.lms.repository.UserRepository;
import com.lms.security.JwtService;
import com.lms.service.AuthService;
import com.lms.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiration-minutes:5}")
    private long otpExpiryMinutes;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Registration attempt username={}, email={}, role={}",
                request.getUsername(), request.getEmail(), request.getRole());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username already exists username={}", request.getUsername());
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists email={}", request.getEmail());
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        // Auto-send OTP on successful registration using registered DB email.
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        user.setOtp(passwordEncoder.encode(otp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        User saved = userRepository.save(user);
        String registrationMessage = "Registration successful. Continue to OTP verification.";
        if (!mailEnabled) {
            log.warn("Mail disabled: OTP for username={} is {}", saved.getUsername(), otp);
            log.info("Registration successful (mail disabled) username={}, role={}", saved.getUsername(), saved.getRole());
            registrationMessage = "Registration successful. Email sending is disabled, so use the OTP from backend logs.";
        } else {
            try {
                emailService.sendOtpMail(saved.getEmail(), saved.getUsername(), otp);
                log.info("Registration successful and OTP sent username={}, role={}", saved.getUsername(), saved.getRole());
            } catch (MailException ex) {
                log.error("Registration created but OTP email failed username={}, email={}, reason={}",
                        saved.getUsername(), saved.getEmail(), ex.getMessage());
                log.warn("Fallback OTP for username={} is {}", saved.getUsername(), otp);
                registrationMessage = "Registration successful, but OTP email could not be sent. Use the OTP from backend logs or fix SMTP and request OTP again.";
            }
        }

        return toUserResponse(saved, registrationMessage);
    }

    @Override
    @Transactional
    public ApiMessageResponse sendOtp(OtpSendRequest request) {
        log.info("Login attempt with username={}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Username does not exist"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BadRequestException("Email not found for user");
        }

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        user.setOtp(passwordEncoder.encode(otp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        userRepository.save(user);

        if (!mailEnabled) {
            log.warn("Mail disabled: OTP for username={} is {}", user.getUsername(), otp);
            log.info("OTP generated (mail disabled) for username={}, role={}", user.getUsername(), user.getRole());
            return new ApiMessageResponse("OTP generated. Email sending is disabled, so use the OTP from backend logs.");
        } else {
            try {
                emailService.sendOtpMail(user.getEmail(), user.getUsername(), otp);
                log.info("OTP generated for username={}, role={}", user.getUsername(), user.getRole());
            } catch (MailException ex) {
                log.error("OTP sending failed username={}, email={}, reason={}",
                        user.getUsername(), user.getEmail(), ex.getMessage());
                log.warn("Fallback OTP for username={} is {}", user.getUsername(), otp);
                return new ApiMessageResponse("OTP generated, but email could not be sent. Use the OTP from backend logs.");
            }
        }
        return new ApiMessageResponse("OTP sent to registered email");
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Username does not exist"));

        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            throw new BadRequestException("OTP not requested");
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new BadRequestException("OTP expired");
        }
        if (!passwordEncoder.matches(request.getOtp(), user.getOtp())) {
            throw new UnauthorizedException("Invalid OTP");
        }

        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .username(user.getUsername())
                .build();
    }
    

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user, null);
    }

    private UserResponse toUserResponse(User user, String message) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .message(message)
                .build();
    }
}
