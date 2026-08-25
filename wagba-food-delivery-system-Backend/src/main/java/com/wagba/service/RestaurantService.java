package com.wagba.service;

import com.wagba.dto.PageResponse;
import com.wagba.dto.food.FoodResponse;
import com.wagba.dto.restaurant.CategoryResponse;
import com.wagba.dto.restaurant.RestaurantResponse;
import com.wagba.dto.restaurant.RestaurantUpdateRequest;
import com.wagba.entity.Category;
import com.wagba.entity.Food;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.repository.CategoryRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             CategoryRepository categoryRepository,
                             FoodRepository foodRepository,
                             UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.foodRepository = foodRepository;
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

    // ---------- Public browsing ----------

    public List<RestaurantResponse> getApprovedRestaurants() {
        List<Restaurant> restaurants = restaurantRepository.findByStatus(RestaurantStatus.APPROVED);
        return restaurants.stream().map(this::toResponse).toList();
    }

    public PageResponse<RestaurantResponse> searchRestaurants(String search, Long categoryId, Boolean offers, Pageable pageable) {
        List<Restaurant> base;
        if (search != null && !search.isBlank()) {
            base = restaurantRepository.adminSearch(RestaurantStatus.APPROVED, search.toLowerCase(), pageable).getContent();
        } else {
            base = restaurantRepository.findByStatus(RestaurantStatus.APPROVED);
        }
        if (categoryId != null) {
            List<Restaurant> byCat = restaurantRepository.findApprovedByCategory(categoryId);
            base.retainAll(byCat);
        }
        if (Boolean.TRUE.equals(offers)) {
            List<Restaurant> withOffers = restaurantRepository.findApprovedWithOffers();
            base.retainAll(withOffers);
        }
        int total = base.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Restaurant> pageContent = start >= total ? List.of() : base.subList(start, end);
        List<RestaurantResponse> content = pageContent.stream().map(this::toResponse).toList();
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total, (int) Math.ceil((double) total / pageable.getPageSize()));
    }

    public RestaurantResponse getRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return toResponse(restaurant);
    }

    public List<FoodResponse> getRestaurantFoods(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        List<Food> foods = foodRepository.findByCategoryRestaurant(restaurant);
        return foods.stream().map(this::toFoodResponse).toList();
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
    }

    // ---------- Owner management ----------

    public RestaurantResponse getMyRestaurant(String email) {
        User owner = loadCurrentUser(email);
        return toResponse(getOwnRestaurant(owner));
    }

    public RestaurantResponse updateMyRestaurant(String email, RestaurantUpdateRequest request) {
        User owner = loadCurrentUser(email);
        Restaurant restaurant = getOwnRestaurant(owner);

        if (request.getName() != null) restaurant.setName(request.getName());
        if (request.getDescription() != null) restaurant.setDescription(request.getDescription());
        if (request.getImageUrl() != null) restaurant.setImageUrl(request.getImageUrl());
        if (request.getCuisine() != null) restaurant.setCuisine(request.getCuisine());
        if (request.getEtaMinutes() != null) restaurant.setEtaMinutes(request.getEtaMinutes());
        if (request.getDeliveryFee() != null) restaurant.setDeliveryFee(request.getDeliveryFee());
        if (request.getMinOrderTotal() != null) restaurant.setMinOrderTotal(request.getMinOrderTotal());
        if (request.getPhone() != null) restaurant.setPhone(request.getPhone());
        if (request.getCity() != null) restaurant.setCity(request.getCity());
        if (request.getStreet() != null) restaurant.setStreet(request.getStreet());
        if (request.getBuildingNumber() != null) restaurant.setBuildingNumber(request.getBuildingNumber());
        if (request.getApartment() != null) restaurant.setApartment(request.getApartment());
        if (request.getDetails() != null) restaurant.setDetails(request.getDetails());
        if (request.getLatitude() != null) restaurant.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) restaurant.setLongitude(request.getLongitude());

        restaurantRepository.save(restaurant);
        return toResponse(restaurant);
    }

    public void deleteMyRestaurant(String email) {
        User owner = loadCurrentUser(email);
        Restaurant restaurant = getOwnRestaurant(owner);
        restaurantRepository.delete(restaurant);
    }

    public CategoryResponse addCategory(String email, String name) {
        User owner = loadCurrentUser(email);
        Restaurant restaurant = getOwnRestaurant(owner);

        if (categoryRepository.existsByNameAndRestaurant(name, restaurant)) {
            throw new RuntimeException("Category already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setRestaurant(restaurant);
        categoryRepository.save(category);

        return new CategoryResponse(category.getId(), category.getName());
    }

    public CategoryResponse updateCategory(String email, Long categoryId, String name) {
        User owner = loadCurrentUser(email);
        Restaurant restaurant = getOwnRestaurant(owner);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getRestaurant().equals(restaurant)) {
            throw new RuntimeException("Category does not belong to your restaurant");
        }

        category.setName(name);
        categoryRepository.save(category);
        return new CategoryResponse(category.getId(), category.getName());
    }

    public void deleteCategory(String email, Long categoryId) {
        User owner = loadCurrentUser(email);
        Restaurant restaurant = getOwnRestaurant(owner);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getRestaurant().equals(restaurant)) {
            throw new RuntimeException("Category does not belong to your restaurant");
        }

        categoryRepository.delete(category);
    }

    // ---------- Mapping ----------

    private RestaurantResponse toResponse(Restaurant restaurant) {
        List<Category> categories = categoryRepository.findByRestaurant(restaurant);
        List<CategoryResponse> categoryResponses = categories.stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
        boolean hasOffers = foodRepository.existsByCategoryRestaurantAndDiscountPriceIsNotNull(restaurant);
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getImageUrl(),
                restaurant.getStatus(),
                categoryResponses,
                restaurant.getCuisine(),
                restaurant.getEtaMinutes(),
                restaurant.getDeliveryFee(),
                restaurant.getMinOrderTotal(),
                restaurant.getAvgRating(),
                restaurant.getPhone(),
                restaurant.getCity(),
                restaurant.getStreet(),
                restaurant.getBuildingNumber(),
                restaurant.getApartment(),
                restaurant.getDetails(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                hasOffers
        );
    }

    private FoodResponse toFoodResponse(Food food) {
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
