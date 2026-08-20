package com.wagba.controller.driver;

import com.wagba.dto.driver.DriverProfileRequest;
import com.wagba.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
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
}