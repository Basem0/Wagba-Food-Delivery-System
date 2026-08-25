package com.wagba.controller.restaurant;

import com.wagba.dto.PageResponse;
import com.wagba.dto.food.FoodResponse;
import com.wagba.dto.restaurant.CategoryResponse;
import com.wagba.dto.restaurant.RestaurantResponse;
import com.wagba.service.RestaurantService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public PageResponse<RestaurantResponse> listRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean offers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return restaurantService.searchRestaurants(search, categoryId, offers, pageable);
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
