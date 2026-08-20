package com.wagba.service;

import com.wagba.dto.driver.DriverProfileRequest;
import com.wagba.entity.Driver;
import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DriverService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    public DriverService(
            UserRepository userRepository,
            DriverRepository driverRepository
    ) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public void completeDriverProfile(
            Long userId,
            DriverProfileRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Email must be verified first"
            );
        }

        if (user.getRole() != UserRole.DRIVER) {
            throw new RuntimeException(
                    "User is not a driver"
            );
        }

        if (user.getOnboardingStatus()
                != OnboardingStatus.PROFILE_COMPLETION_REQUIRED) {

            throw new RuntimeException(
                    "Driver profile completion is not available"
            );
        }

        if (driverRepository.existsByUser(user)) {
            throw new RuntimeException(
                    "Driver profile already exists"
            );
        }

        Driver driver = new Driver();

        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setNationalId(request.getNationalId());
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setLicenseNumber(request.getLicenseNumber());

        driver.setUser(user);

        driverRepository.save(driver);

        user.setOnboardingStatus(
                OnboardingStatus.COMPLETED
        );

        user.setStatus(UserStatus.PENDING);

        userRepository.save(user);
    }
}