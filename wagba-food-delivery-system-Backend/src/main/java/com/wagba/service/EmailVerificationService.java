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
	public void verifyEmail(String token)
	{
		User user = userRepository.findByVerificationToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
		
		if (user.getVerificationTokenExpiry() == null ||
				user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Verification token has expired");
		}
		
		user.setEmailVerified(true);
		user.setVerificationToken(null);
		user.setVerificationTokenExpiry(null);
		
		userRepository.save(user);
	}
}
