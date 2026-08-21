package com.wagba.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
	
	private final JavaMailSender mailSender;
	
	public EmailService(JavaMailSender mailSender)
	{
		this.mailSender = mailSender;
	}
	
	public void sendVerificationEmail(String recipientEmail, String verificationToken)
	{
		String verificationLink =
                "http://localhost:8081/api/v1/auth/verify-email?token="
                        + verificationToken;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipientEmail);
        message.setSubject("Wagba - Verify Your Email");

        message.setText(
                "Welcome to Wagba!\n\n" +
                "Please verify your email by clicking the link below:\n\n" +
                verificationLink +
                "\n\n" +
                "This link will expire in 24 hours."
        );

        mailSender.send(message);
	}

	public void sendPasswordResetEmail(String recipientEmail, String resetToken)
	{
		String resetLink =
                "http://localhost:8081/api/v1/auth/reset-password?token="
                        + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipientEmail);
        message.setSubject("Wagba - Reset Your Password");

        message.setText(
                "Hello from Wagba!\n\n" +
                "Click the link below to reset your password:\n\n" +
                resetLink +
                "\n\n" +
                "This link will expire in 15 minutes."
        );

        mailSender.send(message);
	}
}
