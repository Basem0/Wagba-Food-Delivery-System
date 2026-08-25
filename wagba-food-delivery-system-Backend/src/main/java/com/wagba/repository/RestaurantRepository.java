package com.wagba.repository;

import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByOwner(User owner);

    boolean existsByOwner(User owner);

    List<Restaurant> findByStatus(RestaurantStatus status);

    List<Restaurant> findByStatusIn(List<RestaurantStatus> statuses);

    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);

    long countByStatus(RestaurantStatus status);

    @Query("SELECT r FROM Restaurant r WHERE (:status IS NULL OR r.status = :status) AND (LOWER(r.name) LIKE %:q% OR LOWER(r.cuisine) LIKE %:q%)")
    Page<Restaurant> adminSearch(@Param("status") RestaurantStatus status, @Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT f.category.restaurant FROM Food f WHERE f.category.id = :categoryId AND f.category.restaurant.status = com.wagba.entity.enums.RestaurantStatus.APPROVED")
    List<Restaurant> findApprovedByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT f.category.restaurant FROM Food f WHERE f.discountPrice IS NOT NULL AND f.category.restaurant.status = com.wagba.entity.enums.RestaurantStatus.APPROVED")
    List<Restaurant> findApprovedWithOffers();
}
