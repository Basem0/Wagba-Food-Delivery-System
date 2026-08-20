package com.wagba.repository;

import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
	
    Optional<Restaurant> findByOwner(User owner);

    boolean existsByOwner(User owner);
}
