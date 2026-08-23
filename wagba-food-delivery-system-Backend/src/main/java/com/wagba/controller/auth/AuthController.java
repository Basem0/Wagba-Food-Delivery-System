package com.wagba.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import com.wagba.dto.auth.LoginRequest;
import com.wagba.dto.auth.RegisterRequest;
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

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registeration successful");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully");
    }

    @PostMapping("/select-role")
    public ResponseEntity<String> selectRole(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) String email,
                                            @RequestParam UserRole role) {
        if (userId == null && email != null) {
            userId = userRepository.findByEmail(email).map(User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        authService.selectRole(userId, role);
        return ResponseEntity.ok("Role selected successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        String token = jwtUtil.generateToken(userDetails.getUsername(), role);

        return ResponseEntity.ok(token);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok("If the email exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token,
                                               @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/google-login")
    public ResponseEntity<String> googleLogin(@RequestParam String idToken) {
        String token = authService.googleLogin(idToken);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("role", user.getRole().name());
        result.put("status", user.getStatus().name());
        result.put("phone", user.getPhone());
        return result;
    }

    @PutMapping("/me")
    public Map<String, Object> updateProfile(@RequestBody UserProfileRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        userRepository.save(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("role", user.getRole().name());
        result.put("status", user.getStatus().name());
        result.put("phone", user.getPhone());
        return result;
    }
}