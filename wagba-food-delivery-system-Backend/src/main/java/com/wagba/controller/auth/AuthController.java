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
import com.wagba.entity.enums.UserRole;
import com.wagba.security.JwtUtil;
import com.wagba.service.AuthService;
import com.wagba.service.EmailVerificationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService,
                          EmailVerificationService emailVerificationService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
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
    public ResponseEntity<String> selectRole(@RequestParam Long userId, @RequestParam UserRole role) {
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
}