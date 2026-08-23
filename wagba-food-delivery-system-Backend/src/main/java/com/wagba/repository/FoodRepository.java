package com.wagba.repository;

import com.wagba.entity.Food;
import com.wagba.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long>  {

    List<Food> findByCategoryRestaurant(Restaurant restaurant);

    boolean existsByCategoryRestaurantAndDiscountPriceIsNotNull(Restaurant restaurant);
}
