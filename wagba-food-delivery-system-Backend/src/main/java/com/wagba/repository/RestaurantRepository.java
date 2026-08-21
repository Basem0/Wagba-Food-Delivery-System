package com.wagba.repository;

import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByOwner(User owner);

    boolean existsByOwner(User owner);

    List<Restaurant> findByStatus(RestaurantStatus status);

    List<Restaurant> findByStatusIn(List<RestaurantStatus> statuses);
}
