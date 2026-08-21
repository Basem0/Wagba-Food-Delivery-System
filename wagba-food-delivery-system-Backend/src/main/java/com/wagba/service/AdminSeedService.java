package com.wagba.service;

import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class AdminSeedService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@wagba.com}")
    private String adminEmail;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    public AdminSeedService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seedAdmin() {
        if (userRepository.findByRole(UserRole.ADMIN).isEmpty()) {
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmailVerified(true);
            admin.setOnboardingStatus(OnboardingStatus.COMPLETED);
            userRepository.save(admin);
            System.out.println("Seeded default admin user: " + adminEmail);
        }
    }
}
