package com.wagba.service;

import com.wagba.dto.restaurant.RestaurantUpdateRequest;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;

    public AdminService(UserRepository userRepository,
                        DriverRepository driverRepository,
                        RestaurantRepository restaurantRepository,
                        OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
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
}