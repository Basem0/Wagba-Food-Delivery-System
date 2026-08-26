package com.wagba.repository;

import com.wagba.entity.Order;
import com.wagba.entity.Review;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByOrder(Order order);

    List<Review> findByCustomer(User customer);

    List<Review> findByOrderRestaurant(Restaurant restaurant);

    List<Review> findByOrderIn(List<Order> orders);

    boolean existsByOrderId(Long orderId);

    Optional<Review> findByOrderAndCustomerAndTypeAndTargetId(Order order, User customer, ReviewType type, Long targetId);

    boolean existsByOrderIdAndType(Long orderId, ReviewType type);
}
