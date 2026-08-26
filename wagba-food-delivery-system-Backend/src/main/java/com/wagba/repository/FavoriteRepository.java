package com.wagba.repository;

import com.wagba.entity.Favorite;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserOrderByCreatedAtDesc(User user);
    Optional<Favorite> findByUserAndRestaurant(User user, Restaurant restaurant);
    boolean existsByUserAndRestaurant(User user, Restaurant restaurant);
}
