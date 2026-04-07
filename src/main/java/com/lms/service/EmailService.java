package com.lms.service;

public interface EmailService {
    void sendOtpMail(String toEmail, String username, String otp);
}
