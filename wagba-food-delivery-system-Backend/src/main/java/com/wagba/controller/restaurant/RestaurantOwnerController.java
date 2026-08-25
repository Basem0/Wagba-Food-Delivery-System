package com.wagba.controller.restaurant;

import com.wagba.dto.food.FoodRequest;
import com.wagba.dto.food.FoodResponse;
import com.wagba.dto.restaurant.CategoryRequest;
import com.wagba.dto.restaurant.CategoryResponse;
import com.wagba.dto.restaurant.RestaurantProfileRequest;
import com.wagba.dto.restaurant.RestaurantResponse;
import com.wagba.dto.restaurant.RestaurantUpdateRequest;
import com.wagba.dto.wallet.WithdrawRequest;
import com.wagba.security.SecurityUtil;
import com.wagba.service.FoodService;
import com.wagba.service.RestaurantOwnerService;
import com.wagba.service.RestaurantService;
import com.wagba.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/restaurant-owner")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOwnerController {

    private final RestaurantOwnerService restaurantOwnerService;
    private final RestaurantService restaurantService;
    private final FoodService foodService;
    private final WalletService walletService;

    public RestaurantOwnerController(
            RestaurantOwnerService restaurantOwnerService,
            RestaurantService restaurantService,
            FoodService foodService,
            WalletService walletService
    ) {
        this.restaurantOwnerService = restaurantOwnerService;
        this.restaurantService = restaurantService;
        this.foodService = foodService;
        this.walletService = walletService;
    }

    @PostMapping("/profile")
    public ResponseEntity<String> completeRestaurantProfile(@Valid @RequestBody RestaurantProfileRequest request) {
        // The owner is taken from the JWT, never from the request.
        restaurantOwnerService.completeRestaurantProfile(SecurityUtil.getCurrentUserEmail(), request);
        return ResponseEntity.ok("Restaurant profile submitted successfully");
    }

    @GetMapping("/restaurant")
    public RestaurantResponse getMyRestaurant() {
        return restaurantService.getMyRestaurant(SecurityUtil.getCurrentUserEmail());
    }

    @PutMapping("/restaurant")
    public RestaurantResponse updateMyRestaurant(@RequestBody RestaurantUpdateRequest request) {
        return restaurantService.updateMyRestaurant(SecurityUtil.getCurrentUserEmail(), request);
    }

    @DeleteMapping("/restaurant")
    public ResponseEntity<String> deleteMyRestaurant() {
        restaurantService.deleteMyRestaurant(SecurityUtil.getCurrentUserEmail());
        return ResponseEntity.ok("Restaurant deleted successfully");
    }

    @PostMapping("/categories")
    public CategoryResponse addCategory(@RequestBody CategoryRequest request) {
        return restaurantService.addCategory(SecurityUtil.getCurrentUserEmail(), request.getName());
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return restaurantService.updateCategory(SecurityUtil.getCurrentUserEmail(), id, request.getName());
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        restaurantService.deleteCategory(SecurityUtil.getCurrentUserEmail(), id);
        return ResponseEntity.ok("Category deleted successfully");
    }

    @PostMapping("/foods")
    public FoodResponse addFood(@RequestBody FoodRequest request) {
        return foodService.addFood(SecurityUtil.getCurrentUserEmail(), request);
    }

    @PutMapping("/foods/{id}")
    public FoodResponse updateFood(@PathVariable Long id, @RequestBody FoodRequest request) {
        return foodService.updateFood(SecurityUtil.getCurrentUserEmail(), id, request);
    }

    @DeleteMapping("/foods/{id}")
    public ResponseEntity<String> deleteFood(@PathVariable Long id) {
        foodService.deleteFood(SecurityUtil.getCurrentUserEmail(), id);
        return ResponseEntity.ok("Food deleted successfully");
    }

    @GetMapping("/foods")
    public List<FoodResponse> myFoods() {
        return foodService.getMyRestaurantFoods(SecurityUtil.getCurrentUserEmail());
    }

    @GetMapping("/wallet")
    public com.wagba.dto.wallet.WalletResponse myWallet() {
        return walletService.getWalletInfo(SecurityUtil.getCurrentUserEmail());
    }

    @PostMapping("/wallet/withdraw")
    public Map<String, Object> withdraw(@RequestBody WithdrawRequest request) {
        return walletService.withdraw(SecurityUtil.getCurrentUserEmail(), request.getAmount());
    }
}
