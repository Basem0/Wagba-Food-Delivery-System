package com.wagba.controller.admin;

import com.wagba.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/drivers/{driverId}/approve")
    public ResponseEntity<String> approveDriver(@PathVariable Long driverId) {
        adminService.approveDriver(driverId);
        return ResponseEntity.ok("Driver approved");
    }

    @PostMapping("/drivers/{driverId}/reject")
    public ResponseEntity<String> rejectDriver(@PathVariable Long driverId) {
        adminService.rejectDriver(driverId);
        return ResponseEntity.ok("Driver rejected");
    }

    @PostMapping("/restaurants/{restaurantId}/approve")
    public ResponseEntity<String> approveRestaurant(@PathVariable Long restaurantId) {
        adminService.approveRestaurant(restaurantId);
        return ResponseEntity.ok("Restaurant approved");
    }

    @PostMapping("/restaurants/{restaurantId}/reject")
    public ResponseEntity<String> rejectRestaurant(@PathVariable Long restaurantId) {
        adminService.rejectRestaurant(restaurantId);
        return ResponseEntity.ok("Restaurant rejected");
    }
}