package com.wagba.controller.admin;

import com.wagba.dto.PageResponse;
import com.wagba.dto.order.OrderResponse;
import com.wagba.dto.restaurant.RestaurantUpdateRequest;
import com.wagba.entity.Driver;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.OrderStatus;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import com.wagba.service.AdminService;
import com.wagba.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final FoodRepository foodRepository;
    private final DriverRepository driverRepository;
    private final OrderService orderService;

    public AdminController(AdminService adminService,
                           RestaurantRepository restaurantRepository,
                           UserRepository userRepository,
                           FoodRepository foodRepository,
                           DriverRepository driverRepository,
                           OrderService orderService) {
        this.adminService = adminService;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
        this.driverRepository = driverRepository;
        this.orderService = orderService;
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

    @PostMapping("/drivers/{driverId}/ban")
    public ResponseEntity<String> banDriver(@PathVariable Long driverId) {
        adminService.banDriver(driverId);
        return ResponseEntity.ok("Driver banned");
    }

    @PostMapping("/drivers/{driverId}/unban")
    public ResponseEntity<String> unbanDriver(@PathVariable Long driverId) {
        adminService.unbanDriver(driverId);
        return ResponseEntity.ok("Driver unbanned");
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

    @PostMapping("/restaurants/{restaurantId}/suspend")
    public ResponseEntity<String> suspendRestaurant(@PathVariable Long restaurantId) {
        adminService.suspendRestaurant(restaurantId);
        return ResponseEntity.ok("Restaurant suspended");
    }

    @PostMapping("/restaurants/{restaurantId}/activate")
    public ResponseEntity<String> activateRestaurant(@PathVariable Long restaurantId) {
        adminService.activateRestaurant(restaurantId);
        return ResponseEntity.ok("Restaurant activated");
    }

    @PutMapping("/restaurants/{restaurantId}")
    public ResponseEntity<String> editRestaurant(@PathVariable Long restaurantId,
                                                @RequestBody RestaurantUpdateRequest request) {
        adminService.editRestaurant(restaurantId, request);
        return ResponseEntity.ok("Restaurant updated");
    }

    @DeleteMapping("/restaurants/{restaurantId}")
    public ResponseEntity<String> deleteRestaurant(@PathVariable Long restaurantId) {
        adminService.deleteRestaurant(restaurantId);
        return ResponseEntity.ok("Restaurant deleted");
    }

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<String> suspendUser(@PathVariable Long userId) {
        adminService.suspendUser(userId);
        return ResponseEntity.ok("User suspended");
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok("User activated");
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok("User deleted");
    }

    @GetMapping("/restaurants")
    public PageResponse<Map<String, Object>> listRestaurants(@RequestParam(required = false) RestaurantStatus status,
                                                            @RequestParam(required = false) String search,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Restaurant> p = (search != null && !search.isBlank())
                ? restaurantRepository.adminSearch(status, search.toLowerCase(), pg)
                : (status != null ? restaurantRepository.findByStatus(status, pg) : restaurantRepository.findAll(pg));
        List<Map<String, Object>> content = p.getContent().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("status", r.getStatus().name());
            m.put("ownerEmail", r.getOwner() != null ? r.getOwner().getEmail() : null);
            m.put("cuisine", r.getCuisine());
            m.put("description", r.getDescription());
            m.put("avgRating", r.getAvgRating());
            m.put("phone", r.getPhone());
            m.put("hasOffers", foodRepository.existsByCategoryRestaurantAndDiscountPriceIsNotNull(r));
            return m;
        }).collect(Collectors.toList());
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @GetMapping("/drivers")
    public PageResponse<Map<String, Object>> listDrivers(@RequestParam(required = false) UserStatus status,
                                                        @RequestParam(required = false) String search,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<User> p;
        if (search != null && !search.isBlank()) {
            // Scoped to DRIVER - the unscoped search used to return customers here.
            p = userRepository.searchByRole(UserRole.DRIVER, search.toLowerCase(), pg);
        } else if (status != null) {
            p = userRepository.findByRoleAndStatus(UserRole.DRIVER, status, pg);
        } else {
            p = userRepository.findByRole(UserRole.DRIVER, pg);
        }
        List<Map<String, Object>> content = p.getContent().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus().name());
            Driver drv = driverRepository.findByUser(u).orElse(null);
            if (drv != null) {
                m.put("vehicleType", drv.getVehicleType());
                m.put("vehicleNumber", drv.getVehicleNumber());
                m.put("phoneNumber", drv.getPhoneNumber());
                m.put("licenseNumber", drv.getLicenseNumber());
                m.put("nationalId", drv.getNationalId());
            }
            return m;
        }).collect(Collectors.toList());
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @GetMapping("/users")
    public PageResponse<Map<String, Object>> listUsers(@RequestParam(required = false) UserRole role,
                                                      @RequestParam(required = false) String search,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<User> p;
        if (search != null && !search.isBlank()) {
            p = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pg);
        } else if (role != null) {
            p = userRepository.findByRole(role, pg);
        } else {
            p = userRepository.findAll(pg);
        }
        List<Map<String, Object>> content = p.getContent().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole() != null ? u.getRole().name() : "NONE");
            m.put("status", u.getStatus().name());
            return m;
        }).collect(Collectors.toList());
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    /**
     * The README requires the admin to be able to view every order; there was no
     * endpoint for it at all.
     */
    @GetMapping("/orders")
    public PageResponse<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.allOrders(status, pg);
    }

    /** Headline counts for the dashboard, so the overview does not have to fetch every list. */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customers", userRepository.countByRole(UserRole.CUSTOMER));
        m.put("drivers", userRepository.countByRole(UserRole.DRIVER));
        m.put("owners", userRepository.countByRole(UserRole.RESTAURANT_OWNER));
        m.put("restaurants", restaurantRepository.count());
        m.put("pendingRestaurants", restaurantRepository.countByStatus(RestaurantStatus.PENDING));
        m.put("pendingDrivers", userRepository.findByRoleAndStatus(UserRole.DRIVER, UserStatus.PENDING).size());
        m.put("orders", orderService.countAll());
        m.put("revenue", orderService.totalRevenue());
        return m;
    }
}
