package com.wagba.controller.driver;

import com.wagba.dto.driver.DriverLocationRequest;
import com.wagba.dto.driver.DriverProfileRequest;
import com.wagba.security.SecurityUtil;
import com.wagba.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver")
@PreAuthorize("hasRole('DRIVER')")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/profile")
    public ResponseEntity<String> completeDriverProfile(
            @RequestParam Long userId,
            @RequestBody DriverProfileRequest request
    ) {

        driverService.completeDriverProfile(
                userId,
                request
        );

        return ResponseEntity.ok(
                "Driver profile submitted successfully"
        );
    }

    @PostMapping("/location")
    public ResponseEntity<String> updateLocation(@RequestBody DriverLocationRequest request) {
        driverService.updateLocation(SecurityUtil.getCurrentUserEmail(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok("Location updated");
    }
}