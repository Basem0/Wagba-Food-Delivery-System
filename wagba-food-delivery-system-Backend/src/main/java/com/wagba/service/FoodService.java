package com.wagba.service;

import com.wagba.dto.food.FoodRequest;
import com.wagba.dto.food.FoodResponse;
import com.wagba.entity.Category;
import com.wagba.entity.Food;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.repository.CategoryRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public FoodService(FoodRepository foodRepository,
                       CategoryRepository categoryRepository,
                       RestaurantRepository restaurantRepository,
                       UserRepository userRepository) {
        this.foodRepository = foodRepository;
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    private User loadCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Restaurant getOwnRestaurant(User owner) {
        return restaurantRepository.findByOwner(owner)
                .orElseThrow(() -> new RuntimeException("Restaurant not found for this owner"));
    }

    private Category getOwnCategory(User owner, Long categoryId) {
        Restaurant restaurant = getOwnRestaurant(owner);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getRestaurant().equals(restaurant)) {
            throw new RuntimeException("Category does not belong to your restaurant");
        }
        return category;
    }

    public FoodResponse addFood(String email, FoodRequest request) {
        User owner = loadCurrentUser(email);
        Category category = getOwnCategory(owner, request.getCategoryId());

        Food food = new Food();
        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setPrice(request.getPrice());
        food.setImageUrl(request.getImageUrl());
        food.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        food.setCategory(category);
        foodRepository.save(food);

        return toResponse(food);
    }

    public FoodResponse updateFood(String email, Long foodId, FoodRequest request) {
        User owner = loadCurrentUser(email);
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));

        if (!food.getCategory().getRestaurant().equals(getOwnRestaurant(owner))) {
            throw new RuntimeException("Food does not belong to your restaurant");
        }

        if (request.getName() != null) food.setName(request.getName());
        if (request.getDescription() != null) food.setDescription(request.getDescription());
        if (request.getPrice() != null) food.setPrice(request.getPrice());
        if (request.getImageUrl() != null) food.setImageUrl(request.getImageUrl());
        if (request.getDiscountPrice() != null) food.setDiscountPrice(request.getDiscountPrice());
        if (request.getAvailable() != null) food.setAvailable(request.getAvailable());
        if (request.getCategoryId() != null) {
            Category category = getOwnCategory(owner, request.getCategoryId());
            food.setCategory(category);
        }

        foodRepository.save(food);
        return toResponse(food);
    }

    public void deleteFood(String email, Long foodId) {
        User owner = loadCurrentUser(email);
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));

        if (!food.getCategory().getRestaurant().equals(getOwnRestaurant(owner))) {
            throw new RuntimeException("Food does not belong to your restaurant");
        }

        foodRepository.delete(food);
    }

    public List<FoodResponse> getMyRestaurantFoods(String email) {
        User owner = loadCurrentUser(email);
        Restaurant restaurant = getOwnRestaurant(owner);
        return foodRepository.findByCategoryRestaurant(restaurant).stream()
                .map(this::toResponse)
                .toList();
    }

    private FoodResponse toResponse(Food food) {
        BigDecimal discount = food.getDiscountPrice();
        boolean offer = discount != null && discount.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(food.getPrice()) < 0;
        return new FoodResponse(
                food.getId(),
                food.getName(),
                food.getDescription(),
                food.getPrice(),
                food.getImageUrl(),
                food.isAvailable(),
                food.getCategory().getId(),
                food.getCategory().getName(),
                food.getDiscountPrice(),
                offer
        );
    }
}
