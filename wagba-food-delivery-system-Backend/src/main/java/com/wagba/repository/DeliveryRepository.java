package com.wagba.repository;

import com.wagba.entity.Delivery;
import com.wagba.entity.Order;
import com.wagba.entity.User;
import com.wagba.entity.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByDriver(User driver);

    List<Delivery> findByDriverIsNullAndStatus(DeliveryStatus status);

    Optional<Delivery> findByOrder(Order order);

    Optional<Delivery> findByOrderAndId(Order order, Long id);

    Optional<Delivery> findByDriverAndId(User driver, Long id);

    List<Delivery> findByStatus(DeliveryStatus status);

    Page<Delivery> findByDriver(User driver, Pageable pageable);

    Page<Delivery> findByDriverIsNullAndStatus(DeliveryStatus status, Pageable pageable);

    long countByDriver(User driver);
}
