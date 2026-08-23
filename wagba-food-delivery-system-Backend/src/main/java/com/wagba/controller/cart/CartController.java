package com.wagba.controller.cart;

import com.wagba.dto.cart.CartItemRequest;
import com.wagba.dto.cart.CartResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public CartResponse addItem(@RequestBody CartItemRequest request) {
        return cartService.addItem(SecurityUtil.getCurrentUserEmail(), request);
    }

    @GetMapping("/items")
    public CartResponse getCart() {
        return cartService.getCart(SecurityUtil.getCurrentUserEmail());
    }

    @PutMapping("/items/{id}")
    public CartResponse updateItem(@PathVariable Long id, @RequestBody CartItemRequest request) {
        return cartService.updateItem(SecurityUtil.getCurrentUserEmail(), id, request.getQuantity());
    }

    @DeleteMapping("/items/{id}")
    public CartResponse removeItem(@PathVariable Long id) {
        return cartService.removeItem(SecurityUtil.getCurrentUserEmail(), id);
    }
}
