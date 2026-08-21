package com.wagba.controller.restaurant;

import com.wagba.dto.food.FoodResponse;
import com.wagba.dto.restaurant.CategoryResponse;
import com.wagba.dto.restaurant.RestaurantResponse;
import com.wagba.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public List<RestaurantResponse> listRestaurants() {
        return restaurantService.getApprovedRestaurants();
    }

    @GetMapping("/{id}")
    public RestaurantResponse getRestaurant(@PathVariable Long id) {
        return restaurantService.getRestaurant(id);
    }

    @GetMapping("/{id}/foods")
    public List<FoodResponse> getRestaurantFoods(@PathVariable Long id) {
        return restaurantService.getRestaurantFoods(id);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return restaurantService.getAllCategories();
    }
}
