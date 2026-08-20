package com.wagba.service;

import com.wagba.config.SecurityConfig;

import javax.management.RuntimeErrorException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wagba.dto.auth.RegisterRequest;
import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.UserStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.transaction.Transactional;
import com.wagba.service.EmailService;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	
	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			EmailService emailService) 
	{
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
	}
	
	@Transactional
	public void register(RegisterRequest request) {
		
		// Check if email exist
		if(userRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email Already Exist");
		}
		
		// Create new user
		User user = new User();
		
		user.setName(request.getName());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		
		// initial acc state
		user.setRole(null);
		user.setStatus(UserStatus.PENDING);
		user.setEmailVerified(false);
		user.setOnboardingStatus(OnboardingStatus.ROLE_SELECTION_REQUIRED);
		
		// Email verification
		String verificationToken = UUID.randomUUID().toString();

		user.setVerificationToken(verificationToken);
		user.setVerificationTokenExpiry(
		        LocalDateTime.now().plusHours(24)
		);
		
		
		// save user
		userRepository.save(user);
		
		emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
	}
	
	@Transactional
	public void selectRole(Long userId, UserRole role)
	{
		User user = userRepository.findById(userId)
		        .orElseThrow(() -> new RuntimeException("User not found"));
		
		if(!user.isEmailVerified()) {
			throw new RuntimeException("Email must be verified first");
		}
		
		if(user.getOnboardingStatus() != OnboardingStatus.ROLE_SELECTION_REQUIRED) {
			throw new RuntimeException("Role selection is not available");
		}
		
		if (role == UserRole.CUSTOMER) {

	        user.setRole(UserRole.CUSTOMER);
	        user.setStatus(UserStatus.ACTIVE);
	        user.setOnboardingStatus(OnboardingStatus.COMPLETED);

	    }

	    else if (role == UserRole.RESTAURANT_OWNER ||
	             role == UserRole.DRIVER) {

	        user.setRole(role);
	        user.setStatus(UserStatus.PENDING);
	        user.setOnboardingStatus(OnboardingStatus.PROFILE_COMPLETION_REQUIRED);

	    }

	    else if (role == UserRole.ADMIN) {

	        throw new RuntimeException("You cannot register as ADMIN");
	    }

	    userRepository.save(user);
	}
	
}
