package com.wagba.service;

import com.wagba.dto.PageResponse;
import com.wagba.dto.restaurant.RestaurantUpdateRequest;
import com.wagba.entity.Driver;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import com.wagba.repository.DeliveryRepository;
import com.wagba.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final FoodRepository foodRepository;
    private final DeliveryRepository deliveryRepository;
    private final ReviewRepository reviewRepository;

    public AdminService(UserRepository userRepository,
                        DriverRepository driverRepository,
                        RestaurantRepository restaurantRepository,
                        OrderRepository orderRepository,
                        FoodRepository foodRepository,
                        DeliveryRepository deliveryRepository,
                        ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
        this.foodRepository = foodRepository;
        this.deliveryRepository = deliveryRepository;
        this.reviewRepository = reviewRepository;
    }

    // ---------- List / query methods ----------

    public PageResponse<Map<String, Object>> listRestaurants(RestaurantStatus status, String search, Pageable pg) {
        Page<Restaurant> p = (search != null && !search.isBlank())
                ? restaurantRepository.adminSearch(status, search.toLowerCase(), pg)
                : (status != null ? restaurantRepository.findByStatus(status, pg) : restaurantRepository.findAll(pg));
        var content = p.getContent().stream().map(r -> {
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

    public PageResponse<Map<String, Object>> listDrivers(UserStatus status, String search, Pageable pg) {
        Page<User> p;
        if (search != null && !search.isBlank()) {
            p = userRepository.searchByRole(UserRole.DRIVER, search.toLowerCase(), pg);
        } else if (status != null) {
            p = userRepository.findByRoleAndStatus(UserRole.DRIVER, status, pg);
        } else {
            p = userRepository.findByRole(UserRole.DRIVER, pg);
        }
        var content = p.getContent().stream().map(u -> {
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

    public PageResponse<Map<String, Object>> listUsers(UserRole role, String search, Pageable pg) {
        Page<User> p;
        if (search != null && !search.isBlank()) {
            p = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pg);
        } else if (role != null) {
            p = userRepository.findByRole(role, pg);
        } else {
            p = userRepository.findAll(pg);
        }
        var content = p.getContent().stream().map(u -> {
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

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customers", userRepository.countByRole(UserRole.CUSTOMER));
        m.put("drivers", userRepository.countByRole(UserRole.DRIVER));
        m.put("owners", userRepository.countByRole(UserRole.RESTAURANT_OWNER));
        m.put("restaurants", restaurantRepository.count());
        m.put("pendingRestaurants", restaurantRepository.countByStatus(RestaurantStatus.PENDING));
        m.put("pendingDrivers", userRepository.findByRoleAndStatus(UserRole.DRIVER, UserStatus.PENDING).size());
        m.put("orders", orderRepository.count());
        java.math.BigDecimal revenue = orderRepository.sumTotalByStatus(com.wagba.entity.enums.OrderStatus.DELIVERED);
        m.put("revenue", revenue != null ? revenue : java.math.BigDecimal.ZERO);
        return m;
    }

    /**
     * Note: {@code driverId} is the <em>User</em> id, which is what
     * {@code GET /admin/drivers} returns.
     */
    @Transactional
    public void approveDriver(Long driverId) {
        User user = requireDriver(driverId);
        if (driverRepository.findByUser(user).isEmpty()) {
            throw new RuntimeException(user.getName()
                    + " has not submitted their vehicle details yet, so there is nothing to approve");
        }

        if (user.getStatus() != UserStatus.PENDING) {
            throw new RuntimeException("Only pending drivers can be approved (this one is "
                    + user.getStatus().name().toLowerCase() + ")");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void rejectDriver(Long driverId) {
        User user = requireDriver(driverId);

        if (user.getStatus() != UserStatus.PENDING) {
            throw new RuntimeException("Only pending drivers can be rejected (this one is "
                    + user.getStatus().name().toLowerCase() + ")");
        }

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
    }

    /** Guards the driver endpoints so they cannot be pointed at a customer's id. */
    private User requireDriver(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != UserRole.DRIVER) {
            throw new RuntimeException("User #" + userId + " is not a driver");
        }
        return user;
    }

    @Transactional
    public void approveRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (restaurant.getStatus() != RestaurantStatus.PENDING) {
            throw new RuntimeException("Only pending restaurants can be approved");
        }

        restaurant.setStatus(RestaurantStatus.APPROVED);
        restaurant.getOwner().setStatus(UserStatus.ACTIVE);

        restaurantRepository.save(restaurant);
        userRepository.save(restaurant.getOwner());
    }

    @Transactional
    public void rejectRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (restaurant.getStatus() != RestaurantStatus.PENDING) {
            throw new RuntimeException("Only pending restaurants can be rejected");
        }

        restaurant.setStatus(RestaurantStatus.REJECTED);
        restaurant.getOwner().setStatus(UserStatus.REJECTED);

        restaurantRepository.save(restaurant);
        userRepository.save(restaurant.getOwner());
    }

    // ---------- Suspend / activate users ----------

    @Transactional
    public void suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Administrator accounts cannot be suspended");
        }
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        // An owner who can no longer sign in must not keep taking orders.
        restaurantRepository.findByOwner(user).ifPresent(r -> {
            if (r.getStatus() == RestaurantStatus.APPROVED) {
                r.setStatus(RestaurantStatus.SUSPENDED);
                restaurantRepository.save(r);
            }
        });
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Administrator accounts cannot be deleted");
        }
        if (orderRepository.countByCustomer(user) > 0) {
            throw new RuntimeException("Cannot delete user with existing orders. Suspend instead.");
        }
        // Deleting an owner while the restaurant row still points at them fails on a
        // foreign key, which used to surface as an opaque 500.
        if (restaurantRepository.findByOwner(user).isPresent()) {
            throw new RuntimeException("Delete or reassign this owner's restaurant first, or suspend the account instead.");
        }
        driverRepository.findByUser(user).ifPresent(driverRepository::delete);
        userRepository.delete(user);
    }

    // ---------- Suspend / edit / delete restaurants ----------

    @Transactional
    public void suspendRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        restaurant.setStatus(RestaurantStatus.SUSPENDED);
        restaurantRepository.save(restaurant);
    }

    @Transactional
    public void activateRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        restaurant.setStatus(RestaurantStatus.APPROVED);
        restaurantRepository.save(restaurant);
    }

    @Transactional
    public void editRestaurant(Long restaurantId, RestaurantUpdateRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        if (request.getName() != null) restaurant.setName(request.getName());
        if (request.getDescription() != null) restaurant.setDescription(request.getDescription());
        if (request.getImageUrl() != null) restaurant.setImageUrl(request.getImageUrl());
        if (request.getCuisine() != null) restaurant.setCuisine(request.getCuisine());
        if (request.getEtaMinutes() != null) restaurant.setEtaMinutes(request.getEtaMinutes());
        if (request.getDeliveryFee() != null) restaurant.setDeliveryFee(request.getDeliveryFee());
        if (request.getMinOrderTotal() != null) restaurant.setMinOrderTotal(request.getMinOrderTotal());
        restaurantRepository.save(restaurant);
    }

    @Transactional
    public void deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        if (orderRepository.countByRestaurant(restaurant) > 0) {
            throw new RuntimeException("Cannot delete restaurant with existing orders. Suspend instead.");
        }
        restaurantRepository.delete(restaurant);
    }

    // ---------- Ban / unban drivers ----------

    @Transactional
    public void banDriver(Long driverId) {
        User user = requireDriver(driverId);
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
    }

    @Transactional
    public void unbanDriver(Long driverId) {
        User user = requireDriver(driverId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        m.put("totalCustomers", userRepository.countByRole(UserRole.CUSTOMER));
        m.put("totalDrivers", userRepository.countByRole(UserRole.DRIVER));
        m.put("totalOwners", userRepository.countByRole(UserRole.RESTAURANT_OWNER));
        m.put("totalRestaurants", restaurantRepository.count());
        m.put("totalOrders", orderRepository.count());
        BigDecimal totalRevenue = orderRepository.sumTotalByStatus(com.wagba.entity.enums.OrderStatus.DELIVERED);
        m.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        m.put("ordersToday", orderRepository.countSince(now.toLocalDate().atStartOfDay()));
        BigDecimal revenueToday = orderRepository.sumRevenueSince(now.toLocalDate().atStartOfDay());
        m.put("revenueToday", revenueToday != null ? revenueToday : BigDecimal.ZERO);
        m.put("ordersThisWeek", orderRepository.countSince(now.minusWeeks(1)));
        BigDecimal revenueWeek = orderRepository.sumRevenueSince(now.minusWeeks(1));
        m.put("revenueThisWeek", revenueWeek != null ? revenueWeek : BigDecimal.ZERO);
        m.put("ordersThisMonth", orderRepository.countSince(now.minusMonths(1)));
        BigDecimal revenueMonth = orderRepository.sumRevenueSince(now.minusMonths(1));
        m.put("revenueThisMonth", revenueMonth != null ? revenueMonth : BigDecimal.ZERO);
        return m;
    }

    public Map<String, Object> getDriverPerformance(Long driverId) {
        User user = requireDriver(driverId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("driverId", user.getId());
        m.put("name", user.getName());
        m.put("email", user.getEmail());
        m.put("status", user.getStatus().name());
        com.wagba.entity.Driver drv = driverRepository.findByUser(user).orElse(null);
        if (drv != null) {
            m.put("vehicleType", drv.getVehicleType());
            m.put("vehicleNumber", drv.getVehicleNumber());
        }
        long totalDeliveries = deliveryRepository.countByDriver(user);
        m.put("totalDeliveries", totalDeliveries);
        BigDecimal totalEarnings = deliveryRepository.sumEarningsSince(user, LocalDateTime.MIN);
        m.put("totalEarnings", totalEarnings != null ? totalEarnings : BigDecimal.ZERO);
        com.wagba.entity.Driver drvEntity = driverRepository.findByUser(user).orElse(null);
        if (drvEntity != null) {
            Double avgRating = reviewRepository.avgRatingByDriver(drvEntity.getId());
            m.put("avgRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null);
            long reviewCount = reviewRepository.countByDriver(drvEntity.getId());
            m.put("reviewCount", reviewCount);
        }
        return m;
    }
}