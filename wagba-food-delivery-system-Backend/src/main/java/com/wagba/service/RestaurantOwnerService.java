package com.wagba.service;

import com.wagba.dto.restaurant.RestaurantProfileRequest;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RestaurantOwnerService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public RestaurantOwnerService(
            UserRepository userRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public void completeRestaurantProfile(
            Long userId,
            RestaurantProfileRequest request
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

        if (user.getRole() != UserRole.RESTAURANT_OWNER) {
            throw new RuntimeException(
                    "User is not a restaurant owner"
            );
        }

        if (user.getOnboardingStatus()
                != OnboardingStatus.PROFILE_COMPLETION_REQUIRED) {

            throw new RuntimeException(
                    "Restaurant profile completion is not required"
            );
        }

        if (restaurantRepository.existsByOwner(user)) {
            throw new RuntimeException(
                    "Restaurant profile already exists"
            );
        }

        Restaurant restaurant = new Restaurant();

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setImageUrl(request.getImageUrl());
        restaurant.setCuisine(request.getCuisine());
        restaurant.setEtaMinutes(request.getEtaMinutes());
        restaurant.setDeliveryFee(request.getDeliveryFee());
        restaurant.setMinOrderTotal(request.getMinOrderTotal());
        restaurant.setOwner(user);

        restaurantRepository.save(restaurant);

        user.setOnboardingStatus(
                OnboardingStatus.COMPLETED
        );

        user.setStatus(UserStatus.PENDING);

        userRepository.save(user);
    }
}