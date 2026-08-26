package com.wagba.controller.review;

import com.wagba.dto.review.ReviewRequest;
import com.wagba.dto.review.ReviewResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse create(@RequestBody ReviewRequest request) {
        return reviewService.createReview(SecurityUtil.getCurrentUserEmail(), request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<ReviewResponse> myReviews() {
        return reviewService.getMyReviews(SecurityUtil.getCurrentUserEmail());
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<ReviewResponse> orderReviews(@PathVariable Long orderId) {
        return reviewService.getOrderReviews(SecurityUtil.getCurrentUserEmail(), orderId);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<ReviewResponse> restaurantReviews(@PathVariable Long restaurantId) {
        return reviewService.getRestaurantReviews(restaurantId);
    }

    @GetMapping("/driver/{driverId}")
    public List<ReviewResponse> driverReviews(@PathVariable Long driverId) {
        return reviewService.getDriverReviews(driverId);
    }
}
