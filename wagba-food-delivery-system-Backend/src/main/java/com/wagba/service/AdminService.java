package com.wagba.service;

import com.wagba.dto.restaurant.RestaurantUpdateRequest;
import com.wagba.entity.Driver;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.DeliveryRepository;
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
    private final DeliveryRepository deliveryRepository;

    public AdminService(UserRepository userRepository,
                        DriverRepository driverRepository,
                        RestaurantRepository restaurantRepository,
                        OrderRepository orderRepository,
                        DeliveryRepository deliveryRepository) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public void approveDriver(Long driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new RuntimeException("Only pending drivers can be approved");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void rejectDriver(Long driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new RuntimeException("Only pending drivers can be rejected");
        }

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
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
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getStatus() == UserStatus.REJECTED || user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (orderRepository.countByCustomer(user) > 0) {
            throw new RuntimeException("Cannot delete user with existing orders. Suspend instead.");
        }
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
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
    }

    @Transactional
    public void unbanDriver(Long driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }
}