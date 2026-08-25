package com.wagba.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        /** Items only, before delivery fee and coupons. */
        BigDecimal total,
        Long restaurantId,
        String restaurantName,
        /** Delivery fee that will be charged at checkout, for an accurate cart summary. */
        BigDecimal deliveryFee,
        BigDecimal minOrderTotal,
        /** True when the cart meets the restaurant's minimum and nothing is unavailable. */
        boolean checkoutReady,
        String checkoutBlockedReason
) {
}
