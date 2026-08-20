package com.wagba.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wagba.dto.auth.RegisterRequest;
import com.wagba.entity.enums.UserRole	;
import com.wagba.service.AuthService;
import com.wagba.service.EmailVerificationService;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final EmailVerificationService emailVerificationService;
	
	public AuthController(AuthService authService,
			EmailVerificationService emailVerificationService)
	{
		this.authService = authService;
		this.emailVerificationService = emailVerificationService;
	}
	
	@PostMapping("/register")
	public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request)
	{
		authService.register(request);
		
		return ResponseEntity.status(HttpStatus.CREATED).body("Registeration successful");
	}
	
	@GetMapping("/verify-email")
	public ResponseEntity<String> verifyEmail(
	        @RequestParam String token
	) {

	    emailVerificationService.verifyEmail(token);

	    return ResponseEntity.ok("Email verified successfully");
	}
	
	@PostMapping("/select-role")
	public ResponseEntity<String> selectRole(
	        @RequestParam Long userId,
	        @RequestParam UserRole role
	) {

	    authService.selectRole(userId, role);

	    return ResponseEntity.ok("Role selected successfully");
	}
}
