package com.wagba.service;

import com.wagba.entity.User;
import com.wagba.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {
	
	private final UserRepository userRepository;
	
	public EmailVerificationService(UserRepository userRepository)
	{
		this.userRepository = userRepository;
	}
	
	@Transactional
	public User verifyEmail(String token)
	{
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("Verification code is required");
		}

		User user = userRepository.findByVerificationToken(token.trim())
				.orElseThrow(() -> new IllegalArgumentException("Invalid or already used verification code"));

		if (user.getVerificationTokenExpiry() == null ||
				user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Verification code has expired. Please register again.");
		}

		user.setEmailVerified(true);
		user.setVerificationToken(null);
		user.setVerificationTokenExpiry(null);

		return userRepository.save(user);
	}
}
