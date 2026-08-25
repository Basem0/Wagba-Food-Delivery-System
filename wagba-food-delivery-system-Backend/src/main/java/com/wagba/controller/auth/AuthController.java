package com.wagba.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import com.wagba.dto.auth.AuthResponse;
import com.wagba.dto.auth.ForgotPasswordRequest;
import com.wagba.dto.auth.GoogleLoginRequest;
import com.wagba.dto.auth.LoginRequest;
import com.wagba.dto.auth.RegisterRequest;
import com.wagba.dto.auth.ResetPasswordRequest;
import com.wagba.dto.auth.SelectRoleRequest;
import com.wagba.dto.auth.VerifyEmailRequest;
import com.wagba.dto.user.ChangePasswordRequest;
import com.wagba.dto.user.UserProfileRequest;
import com.wagba.entity.User;
import com.wagba.entity.enums.UserRole;
import com.wagba.repository.UserRepository;
import com.wagba.security.SecurityUtil;
import com.wagba.security.JwtUtil;
import com.wagba.service.AuthService;
import com.wagba.service.EmailVerificationService;
import jakarta.validation.Valid;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          EmailVerificationService emailVerificationService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserRepository userRepository) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    private static Map<String, Object> message(String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", text);
        return body;
    }

    /** Null-safe view of a user - role and status are null for a fresh sign-up. */
    private Map<String, Object> userPayload(User user) {
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

    private User requireCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        Map<String, Object> body = message("Registration successful. Check your email for the verification code.");
        body.put("email", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Verifies the email and, when a role was picked at sign-up, completes
     * onboarding in the same call.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(doVerify(request.getToken()));
    }

    /** Same thing via GET, so the link in the verification email works. */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmailByLink(@RequestParam String token) {
        return ResponseEntity.ok(doVerify(token));
    }

    private Map<String, Object> doVerify(String token) {
        User user = emailVerificationService.verifyEmail(token);
        UserRole applied = authService.applyPendingRole(user.getId());
        user = userRepository.findById(user.getId()).orElse(user);

        Map<String, Object> body = message(applied != null
                ? "Email verified and your " + label(applied) + " account is ready. You can log in now."
                : "Email verified successfully. Please choose your account type.");
        body.put("roleSelectionRequired", applied == null && user.getRole() == null);
        body.put("user", userPayload(user));
        return body;
    }

    private String label(UserRole role) {
        return switch (role) {
            case CUSTOMER -> "customer";
            case RESTAURANT_OWNER -> "restaurant";
            case DRIVER -> "driver";
            case ADMIN -> "admin";
        };
    }

    @PostMapping("/select-role")
    public ResponseEntity<Map<String, Object>> selectRole(@Valid @RequestBody SelectRoleRequest request) {
        Long userId = request.getUserId();
        if (userId == null && request.getEmail() != null && !request.getEmail().isBlank()) {
            userId = userRepository.findByEmail(request.getEmail()).map(User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        if (userId == null) {
            throw new RuntimeException("userId or email is required");
        }
        authService.selectRole(userId, request.getRole());
        return ResponseEntity.ok(message("Role selected successfully. You can log in now."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        String token = jwtUtil.generateToken(userDetails.getUsername(), role);
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getOnboardingStatus() != null ? user.getOnboardingStatus().name() : null
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(message("If that email is registered, a reset code has been sent to it."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(message("Password reset successfully. You can log in now."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(message("Logged out successfully"));
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        String token = authService.googleLogin(request.getIdToken());
        User user = userRepository.findByEmail(jwtUtil.extractEmail(token))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getOnboardingStatus() != null ? user.getOnboardingStatus().name() : null
        ));
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return userPayload(requireCurrentUser());
    }

    @PutMapping("/me")
    public Map<String, Object> updateProfile(@RequestBody UserProfileRequest request) {
        User user = requireCurrentUser();
        if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
        userRepository.save(user);
        return userPayload(user);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        authService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(message("Password changed successfully"));
    }
}
