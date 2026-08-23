package com.wagba.repository;

import com.wagba.entity.Order;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer(User customer);

    List<Order> findByRestaurant(Restaurant restaurant);

    Optional<Order> findByRestaurantAndId(Restaurant restaurant, Long id);

    Optional<Order> findByCustomerAndId(User customer, Long id);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByCustomer(User customer, Pageable pageable);

    Page<Order> findByRestaurant(Restaurant restaurant, Pageable pageable);

    long countByCustomer(User customer);

    long countByRestaurant(Restaurant restaurant);
}
