package com.wagba.repository;

import com.wagba.entity.Category;
import com.wagba.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByRestaurant(Restaurant restaurant);

    boolean existsByNameAndRestaurant(String name, Restaurant restaurant);
}