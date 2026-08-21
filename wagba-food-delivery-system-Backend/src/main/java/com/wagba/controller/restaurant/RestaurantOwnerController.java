package com.wagba.controller.restaurant;

import com.wagba.dto.restaurant.RestaurantProfileRequest;
import com.wagba.service.RestaurantOwnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; 
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/restaurant-owner")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOwnerController {

    private final RestaurantOwnerService restaurantOwnerService;

    public RestaurantOwnerController(RestaurantOwnerService restaurantOwnerService) {
        this.restaurantOwnerService = restaurantOwnerService;
    }

    @PostMapping("/profile")
    public ResponseEntity<String> completeRestaurantProfile(
            @RequestParam Long userId,
            @RequestBody RestaurantProfileRequest request
    ) {
        restaurantOwnerService.completeRestaurantProfile(userId, request);
        return ResponseEntity.ok("Restaurant profile submitted successfully");
    }
}