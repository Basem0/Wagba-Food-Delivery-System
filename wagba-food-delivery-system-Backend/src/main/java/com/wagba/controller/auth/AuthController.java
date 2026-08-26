package com.wagba.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
import com.wagba.security.SecurityUtil;
import com.wagba.service.AuthService;
import com.wagba.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService,
                          EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    private static Map<String, Object> message(String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", text);
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

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        Map<String, Object> body = message("Registration successful. Check your email for the verification code.");
        body.put("email", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(doVerify(request.getToken()));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmailByLink(@RequestParam String token) {
        return ResponseEntity.ok(doVerify(token));
    }

    private Map<String, Object> doVerify(String token) {
        User user = emailVerificationService.verifyEmail(token);
        UserRole applied = authService.applyPendingRole(user.getId());
        user = authService.findByEmail(user.getEmail());

        Map<String, Object> body = message(applied != null
                ? "Email verified and your " + label(applied) + " account is ready. You can log in now."
                : "Email verified successfully. Please choose your account type.");
        body.put("roleSelectionRequired", applied == null && user.getRole() == null);
        body.put("user", authService.userPayload(user));
        return body;
    }

    @PostMapping("/select-role")
    public ResponseEntity<Map<String, Object>> selectRole(@Valid @RequestBody SelectRoleRequest request) {
        Long userId = request.getUserId();
        if (userId == null && request.getEmail() != null && !request.getEmail().isBlank()) {
            User user = authService.findByEmail(request.getEmail());
            userId = user.getId();
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId or email is required");
        }
        authService.selectRole(userId, request.getRole());
        return ResponseEntity.ok(message("Role selected successfully. You can log in now."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
        return ResponseEntity.ok(authService.googleLoginPayload(request.getIdToken()));
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return authService.getCurrentUser(SecurityUtil.getCurrentUserEmail());
    }

    @PutMapping("/me")
    public Map<String, Object> updateProfile(@RequestBody UserProfileRequest request) {
        return authService.updateProfile(SecurityUtil.getCurrentUserEmail(), request);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        authService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(message("Password changed successfully"));
    }
}
