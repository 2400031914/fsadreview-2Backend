package com.lms.service.impl;

import com.lms.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpMail(String toEmail, String username, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("LMS OTP Verification");
        message.setText("Hello " + username + ",\n\nYour OTP is: " + otp + "\nThis OTP expires in 5 minutes.");
        mailSender.send(message);
        log.info("OTP email sent to username={}", username);
    }
}
