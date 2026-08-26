package com.wagba.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wagba.dto.auth.AuthResponse;
import com.wagba.dto.auth.LoginRequest;
import com.wagba.dto.auth.RegisterRequest;
import com.wagba.dto.user.UserProfileRequest;
import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.UserStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.repository.UserRepository;
import com.wagba.security.GoogleTokenVerifier;
import com.wagba.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.transaction.Transactional;

@Service
public class AuthService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final TokenBlacklistService tokenBlacklistService;
	private final JwtUtil jwtUtil;
	private final AuthenticationManager authenticationManager;

	@Value("${google.client.id:}")
	private String googleClientId;
	
	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			EmailService emailService,
			TokenBlacklistService tokenBlacklistService,
			JwtUtil jwtUtil,
			AuthenticationManager authenticationManager)
	{
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.tokenBlacklistService = tokenBlacklistService;
		this.jwtUtil = jwtUtil;
		this.authenticationManager = authenticationManager;
	}

	// ---------- User lookup methods ----------

	public User findByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found"));
	}

	public Map<String, Object> userPayload(User user) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", user.getId());
		result.put("name", user.getName());
		result.put("email", user.getEmail());
		result.put("role", user.getRole() != null ? user.getRole().name() : null);
		result.put("status", user.getStatus() != null ? user.getStatus().name() : null);
		result.put("onboardingStatus", user.getOnboardingStatus() != null ? user.getOnboardingStatus().name() : null);
		result.put("emailVerified", user.isEmailVerified());
		result.put("phone", user.getPhone());
		return result;
	}

	public Map<String, Object> getCurrentUser(String email) {
		User user = findByEmail(email);
		return userPayload(user);
	}

	@Transactional
	public Map<String, Object> updateProfile(String email, UserProfileRequest request) {
		User user = findByEmail(email);
		if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName().trim());
		if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
		userRepository.save(user);
		return userPayload(user);
	}

	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		String role = userDetails.getAuthorities().iterator().next().getAuthority();

		String token = jwtUtil.generateToken(userDetails.getUsername(), role);
		User user = findByEmail(userDetails.getUsername());

		return new AuthResponse(
				token,
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole() != null ? user.getRole().name() : null,
				user.getStatus() != null ? user.getStatus().name() : null,
				user.getOnboardingStatus() != null ? user.getOnboardingStatus().name() : null
		);
	}

	public AuthResponse googleLoginPayload(String idToken) {
		String token = googleLogin(idToken);
		User user = findByEmail(jwtUtil.extractEmail(token));
		return new AuthResponse(
				token,
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole() != null ? user.getRole().name() : null,
				user.getStatus() != null ? user.getStatus().name() : null,
				user.getOnboardingStatus() != null ? user.getOnboardingStatus().name() : null
		);
	}

	@Transactional
	public void selectRoleByEmail(String email, UserRole role) {
		User user = findByEmail(email);
		selectRole(user.getId(), role);
	}

	// ---------- Existing methods ----------

	@Transactional
	public void register(RegisterRequest request) {

		// Check if email exist
		if(userRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email Already Exist");
		}

		if (request.getRole() == UserRole.ADMIN) {
			throw new RuntimeException("You cannot register as ADMIN");
		}

		// Create new user
		User user = new User();

		user.setName(request.getName());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setPhone(request.getPhone());

		// initial acc state
		user.setRole(null);
		// Remembered so email verification can finish onboarding without asking again.
		user.setPendingRole(request.getRole());
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
			log.warn("Email sending failed (registration still succeeded): {}", e.getMessage());
		}
	}

	/**
	 * Applies the role chosen at sign-up, if any. Called right after the email is
	 * verified so the user is not asked to pick a role a second time.
	 *
	 * @return the role that was applied, or null if there was nothing to apply
	 */
	@Transactional
	public UserRole applyPendingRole(Long userId) {
		User user = userRepository.findById(userId)
		        .orElseThrow(() -> new RuntimeException("User not found"));
		UserRole pending = user.getPendingRole();
		if (pending == null || user.getOnboardingStatus() != OnboardingStatus.ROLE_SELECTION_REQUIRED) {
			return null;
		}
		selectRole(userId, pending);
		user.setPendingRole(null);
		userRepository.save(user);
		return pending;
	}

	@Transactional
	public void selectRole(Long userId, UserRole role)
	{
		if (userId == null) {
			throw new RuntimeException("User not found");
		}
		if (role == null) {
			throw new RuntimeException("Role is required");
		}
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
	        // Stays PENDING until an admin approves; they can still log in to
	        // complete their profile (see UserDetailsServiceImpl).
	        user.setStatus(UserStatus.PENDING);
	        user.setOnboardingStatus(OnboardingStatus.PROFILE_COMPLETION_REQUIRED);

	    }

	    else if (role == UserRole.ADMIN) {

	        throw new RuntimeException("You cannot register as ADMIN");
	    }

	    user.setPendingRole(null);
	    userRepository.save(user);
	}

	@Transactional
	public void forgotPassword(String email) {
		// Deliberately does not reveal whether the address exists.
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) {
			log.info("Password reset requested for unknown email");
			return;
		}

		String resetToken = UUID.randomUUID().toString();
		user.setPasswordResetToken(resetToken);
		user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
		userRepository.save(user);

		try {
			emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
		} catch (Exception e) {
			log.warn("Reset email failed: {}", e.getMessage());
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

	@Transactional
	public void changePassword(String email, String currentPassword, String newPassword) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found"));
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new RuntimeException("Current password is incorrect");
		}
		user.setPassword(passwordEncoder.encode(newPassword));
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
