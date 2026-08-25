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
    /**
     * Takes the caller's email rather than a userId: the controller used to accept
     * the id as a request parameter, so any signed-in owner could create a
     * restaurant under someone else's account.
     */
    public void completeRestaurantProfile(String email, RestaurantProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

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

        if (restaurantRepository.existsByOwner(user)) {
            throw new RuntimeException(
                    "Restaurant profile already exists"
            );
        }

        if (user.getOnboardingStatus() == OnboardingStatus.ROLE_SELECTION_REQUIRED) {
            throw new RuntimeException(
                    "Choose your account type before submitting a restaurant profile"
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
        restaurant.setPhone(request.getPhone());
        restaurant.setCity(request.getCity());
        restaurant.setStreet(request.getStreet());
        restaurant.setBuildingNumber(request.getBuildingNumber());
        restaurant.setDetails(request.getDetails());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setOwner(user);

        restaurantRepository.save(restaurant);

        user.setOnboardingStatus(
                OnboardingStatus.COMPLETED
        );

        // Waits for an admin to approve the restaurant; AdminService.approveRestaurant
        // flips both the restaurant and the owner to ACTIVE.
        user.setStatus(UserStatus.PENDING);

        userRepository.save(user);
    }
}