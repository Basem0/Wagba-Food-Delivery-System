package com.wagba.service;

import com.wagba.config.SecurityConfig;

import javax.management.RuntimeErrorException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wagba.dto.auth.RegisterRequest;
import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.UserStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.repository.UserRepository;
import com.wagba.security.GoogleTokenVerifier;
import com.wagba.security.JwtUtil;
import com.wagba.service.TokenBlacklistService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.transaction.Transactional;
import com.wagba.service.EmailService;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final TokenBlacklistService tokenBlacklistService;
	private final JwtUtil jwtUtil;

	@Value("${google.client.id:}")
	private String googleClientId;
	
	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			EmailService emailService,
			TokenBlacklistService tokenBlacklistService,
			JwtUtil jwtUtil)
	{
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.tokenBlacklistService = tokenBlacklistService;
		this.jwtUtil = jwtUtil;
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
		
		try {
			emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
		} catch (Exception e) {
			System.out.println("Email sending failed (registration still succeeded): " + e.getMessage());
		}
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

	@Transactional
	public void forgotPassword(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found"));

		String resetToken = UUID.randomUUID().toString();
		user.setPasswordResetToken(resetToken);
		user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
		userRepository.save(user);

		try {
			emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
		} catch (Exception e) {
			System.out.println("Reset email failed: " + e.getMessage());
		}
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {
		User user = userRepository.findByPasswordResetToken(token)
				.orElseThrow(() -> new RuntimeException("Invalid reset token"));

		if (user.getPasswordResetTokenExpiry() != null
				&& user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("Reset token expired");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		user.setPasswordResetToken(null);
		user.setPasswordResetTokenExpiry(null);
		userRepository.save(user);
	}

	public void logout(String token) {
		try {
			String jti = jwtUtil.extractJti(token);
			tokenBlacklistService.blacklist(jti);
		} catch (Exception e) {
			// invalid token, nothing to blacklist
		}
	}

	@Transactional
	public String googleLogin(String idToken) {
		if (googleClientId == null || googleClientId.isBlank()) {
			throw new RuntimeException("Google login is not configured");
		}

		GoogleTokenVerifier verifier = new GoogleTokenVerifier(googleClientId);
		String email = verifier.verifyAndGetEmail(idToken);

		User user = userRepository.findByEmail(email).orElse(null);

		if (user == null) {
			user = new User();
			user.setName(email);
			user.setEmail(email);
			user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
			user.setRole(UserRole.CUSTOMER);
			user.setStatus(UserStatus.ACTIVE);
			user.setEmailVerified(true);
			user.setOnboardingStatus(OnboardingStatus.COMPLETED);
			userRepository.save(user);
		}

		String role = "ROLE_" + user.getRole().name();
		return jwtUtil.generateToken(email, role);
	}

}
