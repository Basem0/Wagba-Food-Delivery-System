package com.wagba.controller.restaurant;

import com.wagba.dto.restaurant.RestaurantProfileRequest;
import com.wagba.service.RestaurantOwnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant-owner")
public class RestaurantOwnerController {

    private final RestaurantOwnerService restaurantOwnerService;

    public RestaurantOwnerController(
            RestaurantOwnerService restaurantOwnerService
    ) {
        this.restaurantOwnerService = restaurantOwnerService;
    }

    @PostMapping("/profile")
    public ResponseEntity<String> completeRestaurantProfile(
            @RequestParam Long userId,
            @RequestBody RestaurantProfileRequest request
    ) {

        restaurantOwnerService.completeRestaurantProfile(
                userId,
                request
        );

        return ResponseEntity.ok(
                "Restaurant profile submitted successfully"
        );
    }
}