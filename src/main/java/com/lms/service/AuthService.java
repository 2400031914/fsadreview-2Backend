package com.lms.service;

import com.lms.dto.ApiMessageResponse;
import com.lms.dto.AuthResponse;
import com.lms.dto.OtpSendRequest;
import com.lms.dto.OtpVerifyRequest;
import com.lms.dto.RegisterRequest;
import com.lms.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    ApiMessageResponse sendOtp(OtpSendRequest request);
    AuthResponse verifyOtp(OtpVerifyRequest request);
    UserResponse getCurrentUser(String username);
}
