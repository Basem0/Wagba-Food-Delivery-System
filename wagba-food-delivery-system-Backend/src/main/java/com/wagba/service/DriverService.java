package com.wagba.service;

import com.wagba.dto.driver.DriverProfileRequest;
import com.wagba.dto.driver.DriverResponse;
import com.wagba.entity.Driver;
import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.UserRepository;
import com.wagba.repository.DriverRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    @Transactional
    public void updateLocation(String email, Double latitude, Double longitude) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver profile not found"));
        driver.setLatitude(latitude);
        driver.setLongitude(longitude);
        driver.setLocationUpdatedAt(LocalDateTime.now());
        driverRepository.save(driver);
    }

    public DriverResponse getDriverProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver profile not found"));
        return new DriverResponse(
                driver.getId(),
                driver.getPhoneNumber(),
                driver.getNationalId(),
                driver.getVehicleType(),
                driver.getVehicleNumber(),
                driver.getLicenseNumber(),
                driver.getLatitude(),
                driver.getLongitude()
        );
    }

    @Transactional
    public void updateDriverProfile(String email, DriverProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver profile not found"));
        if (request.getPhoneNumber() != null) driver.setPhoneNumber(request.getPhoneNumber());
        if (request.getNationalId() != null) driver.setNationalId(request.getNationalId());
        if (request.getVehicleType() != null) driver.setVehicleType(request.getVehicleType());
        if (request.getVehicleNumber() != null) driver.setVehicleNumber(request.getVehicleNumber());
        if (request.getLicenseNumber() != null) driver.setLicenseNumber(request.getLicenseNumber());
        driverRepository.save(driver);
    }
}