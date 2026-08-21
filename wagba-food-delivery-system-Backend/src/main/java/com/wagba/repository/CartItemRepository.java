package com.wagba.repository;

import com.wagba.entity.Cart;
import com.wagba.entity.CartItem;
import com.wagba.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByCartAndFood(Cart cart, Food food);

    Optional<CartItem> findByCartAndId(Cart cart, Long id);
}
