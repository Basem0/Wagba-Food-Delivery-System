package com.wagba.service;

import com.wagba.dto.cart.CartItemRequest;
import com.wagba.dto.cart.CartItemResponse;
import com.wagba.dto.cart.CartResponse;
import com.wagba.entity.Cart;
import com.wagba.entity.CartItem;
import com.wagba.entity.Food;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.User;
import com.wagba.repository.CartItemRepository;
import com.wagba.repository.CartRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       FoodRepository foodRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    public CartResponse addItem(String email, CartItemRequest request) {
        User user = currentUser(email);
        Cart cart = getOrCreateCart(user);
        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new RuntimeException("Food not found"));
        if (food.getCategory().getRestaurant().getStatus() != RestaurantStatus.APPROVED) {
            throw new RuntimeException("Food is not available");
        }
        CartItem existing = cartItemRepository.findByCartAndFood(cart, food).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setFood(food);
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }
        cart = cartRepository.findByUser(user).orElse(cart);
        return toResponse(cart);
    }

    public CartResponse getCart(String email) {
        User user = currentUser(email);
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    public CartResponse updateItem(String email, Long itemId, Integer quantity) {
        User user = currentUser(email);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        CartItem item = cartItemRepository.findByCartAndId(cart, itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    public CartResponse removeItem(String email, Long itemId) {
        User user = currentUser(email);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        CartItem item = cartItemRepository.findByCartAndId(cart, itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            BigDecimal subtotal = ci.getFood().getPrice()
                    .multiply(BigDecimal.valueOf(ci.getQuantity()));
            items.add(new CartItemResponse(
                    ci.getId(),
                    ci.getFood().getId(),
                    ci.getFood().getName(),
                    ci.getFood().getPrice(),
                    ci.getQuantity(),
                    subtotal
            ));
            total = total.add(subtotal);
        }
        return new CartResponse(cart.getId(), items, total);
    }
}
