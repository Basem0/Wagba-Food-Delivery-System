package com.wagba.controller.admin;

import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import com.wagba.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService,
                           RestaurantRepository restaurantRepository,
                           UserRepository userRepository) {
        this.adminService = adminService;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
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

    @GetMapping("/restaurants")
    public List<Map<String, Object>> listRestaurants(@RequestParam(required = false) RestaurantStatus status) {
        List<Restaurant> list = (status == null)
                ? restaurantRepository.findAll()
                : restaurantRepository.findByStatus(status);
        return list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("status", r.getStatus().name());
            m.put("ownerEmail", r.getOwner() != null ? r.getOwner().getEmail() : null);
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/drivers")
    public List<Map<String, Object>> listDrivers(@RequestParam(required = false) UserStatus status) {
        List<User> list = (status == null)
                ? userRepository.findByRole(UserRole.DRIVER)
                : userRepository.findByRoleAndStatus(UserRole.DRIVER, status);
        return list.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus().name());
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers(@RequestParam(required = false) UserRole role) {
        List<User> list = (role == null)
                ? userRepository.findAll()
                : userRepository.findByRole(role);
        return list.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole() != null ? u.getRole().name() : "NONE");
            m.put("status", u.getStatus().name());
            return m;
        }).collect(Collectors.toList());
    }
}