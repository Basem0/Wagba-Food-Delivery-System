package com.wagba.service;

import com.wagba.dto.review.ReviewRequest;
import com.wagba.dto.review.ReviewResponse;
import com.wagba.entity.Delivery;
import com.wagba.entity.Order;
import com.wagba.entity.Restaurant;
import com.wagba.entity.Review;
import com.wagba.entity.User;
import com.wagba.entity.enums.OrderStatus;
import com.wagba.repository.DeliveryRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.ReviewRepository;
import com.wagba.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderRepository orderRepository,
                         RestaurantRepository restaurantRepository,
                         DeliveryRepository deliveryRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public ReviewResponse createReview(String email, ReviewRequest request) {
        User customer = currentUser(email);
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Order does not belong to you");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("You can only review delivered orders");
        }
        if (reviewRepository.findByOrder(order).stream().findAny().isPresent()) {
            throw new RuntimeException("Review already exists for this order");
        }
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        Review review = new Review();
        review.setOrder(order);
        review.setCustomer(customer);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review = reviewRepository.save(review);
        return toResponse(review);
    }

    public ReviewResponse getOrderReview(String email, Long orderId) {
        User customer = currentUser(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Order does not belong to you");
        }
        return reviewRepository.findByOrder(order).stream()
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Review not found"));
    }

    public List<ReviewResponse> getMyReviews(String email) {
        User customer = currentUser(email);
        return reviewRepository.findByCustomer(customer).stream().map(this::toResponse).toList();
    }

    public List<ReviewResponse> getRestaurantReviews(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return reviewRepository.findByOrderRestaurant(restaurant).stream().map(this::toResponse).toList();
    }

    public List<ReviewResponse> getDriverReviews(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        List<Delivery> deliveries = deliveryRepository.findByDriver(driver);
        List<Order> orders = new ArrayList<>();
        for (Delivery d : deliveries) {
            orders.add(d.getOrder());
        }
        return reviewRepository.findByOrderIn(orders).stream().map(this::toResponse).toList();
    }

    private ReviewResponse toResponse(Review review) {
        Order order = review.getOrder();
        Long driverId = deliveryRepository.findByOrder(order)
                .map(d -> d.getDriver() != null ? d.getDriver().getId() : null)
                .orElse(null);
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getCustomer().getName(),
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                driverId,
                review.getCreatedAt() != null ? review.getCreatedAt().toString() : null
        );
    }
}
