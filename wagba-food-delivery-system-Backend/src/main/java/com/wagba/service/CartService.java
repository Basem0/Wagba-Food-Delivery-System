package com.wagba.service;

import com.wagba.dto.cart.CartItemRequest;
import com.wagba.dto.cart.CartItemResponse;
import com.wagba.dto.cart.CartResponse;
import com.wagba.entity.Cart;
import com.wagba.entity.CartItem;
import com.wagba.entity.Food;
import com.wagba.entity.Restaurant;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.User;
import com.wagba.repository.CartItemRepository;
import com.wagba.repository.CartRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;

    /** Fallback when a restaurant has not set its own fee - must match OrderService. */
    @Value("${wagba.delivery.fee:15}")
    private BigDecimal defaultDeliveryFee;

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
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }
        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new RuntimeException("Food not found"));
        Restaurant restaurant = food.getCategory().getRestaurant();
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new RuntimeException(restaurant.getName() + " is not accepting orders right now");
        }
        // The README requires that an order never references an unavailable item;
        // rejecting it here means the customer finds out before checkout.
        if (!food.isAvailable()) {
            throw new RuntimeException("\"" + food.getName() + "\" is currently unavailable");
        }
        // A cart can only be checked out against one restaurant, so say so up front
        // instead of letting the customer discover it at checkout.
        Restaurant existingRestaurant = cartRestaurant(cart);
        if (existingRestaurant != null && !existingRestaurant.getId().equals(restaurant.getId())) {
            throw new RuntimeException("Your cart already has items from " + existingRestaurant.getName()
                    + ". Clear it before ordering from " + restaurant.getName() + ".");
        }

        CartItem existing = cartItemRepository.findByCartAndFood(cart, food).orElse(null);
        if (existing != null) {
            existing.setQuantity(Math.min(99, existing.getQuantity() + quantity));
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setFood(food);
            item.setQuantity(Math.min(99, quantity));
            cartItemRepository.save(item);
        }
        cart = cartRepository.findByUser(user).orElse(cart);
        return toResponse(cart);
    }

    private Restaurant cartRestaurant(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) return null;
        return cart.getItems().get(0).getFood().getCategory().getRestaurant();
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
        if (quantity == null) {
            throw new RuntimeException("Quantity is required");
        }
        // Quantity 0 (or below) used to be stored verbatim, producing a zero-priced
        // line item; treat it as "remove" instead.
        if (quantity < 1) {
            return removeItem(email, itemId);
        }
        item.setQuantity(Math.min(99, quantity));
        cartItemRepository.save(item);
        return toResponse(cartRepository.findByUser(user).orElse(cart));
    }

    public CartResponse removeItem(String email, Long itemId) {
        User user = currentUser(email);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        CartItem item = cartItemRepository.findByCartAndId(cart, itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        cart.getItems().remove(item);
        cartRepository.save(cart);
        return toResponse(cartRepository.findByUser(user).orElse(cart));
    }

    public CartResponse clear(String email) {
        User user = currentUser(email);
        Cart cart = getOrCreateCart(user);
        if (!cart.getItems().isEmpty()) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
        return toResponse(cartRepository.findByUser(user).orElse(cart));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean hasUnavailable = false;
        for (CartItem ci : cart.getItems()) {
            Food food = ci.getFood();
            // Charge the offer price when there is one - this used to always read
            // getPrice(), so items on offer were billed at full price.
            BigDecimal unitPrice = OrderService.effectivePrice(food);
            boolean discounted = unitPrice.compareTo(food.getPrice()) < 0;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (!food.isAvailable()) hasUnavailable = true;
        items.add(new CartItemResponse(
                ci.getId(),
                ci.getFood().getId(),
                ci.getFood().getName(),
                ci.getFood().getImageUrl(),
                unitPrice,
                ci.getFood().getPrice(),
                ci.getQuantity(),
                subtotal,
                ci.getFood().isAvailable(),
                ci.getFood().getCategory().getRestaurant().getId()
        ));
            total = total.add(subtotal);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        Restaurant restaurant = cartRestaurant(cart);
        BigDecimal deliveryFee = null;
        BigDecimal minOrderTotal = null;
        String blocked = null;
        if (restaurant != null) {
            deliveryFee = restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : defaultDeliveryFee;
            minOrderTotal = restaurant.getMinOrderTotal();
            if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
                blocked = restaurant.getName() + " is not accepting orders right now";
            } else if (hasUnavailable) {
                blocked = "Some items are no longer available. Please remove them to continue.";
            } else if (minOrderTotal != null && total.compareTo(minOrderTotal) < 0) {
                blocked = "Minimum order is " + minOrderTotal.setScale(2, RoundingMode.HALF_UP)
                        + " EGP - add " + minOrderTotal.subtract(total).setScale(2, RoundingMode.HALF_UP) + " EGP more";
            }
        } else {
            blocked = "Your cart is empty";
        }

        return new CartResponse(
                cart.getId(),
                items,
                total,
                restaurant != null ? restaurant.getId() : null,
                restaurant != null ? restaurant.getName() : null,
                deliveryFee,
                minOrderTotal,
                blocked == null,
                blocked
        );
    }
}
