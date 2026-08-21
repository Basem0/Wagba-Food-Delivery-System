package com.wagba.service;

import com.wagba.entity.Driver;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RestaurantRepository restaurantRepository;

    public AdminService(UserRepository userRepository,
                        DriverRepository driverRepository,
                        RestaurantRepository restaurantRepository) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public void approveDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        User user = driver.getUser();

        if (user.getStatus() != UserStatus.PENDING) {
            throw new RuntimeException("Only pending drivers can be approved");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void rejectDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        User user = driver.getUser();

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
}