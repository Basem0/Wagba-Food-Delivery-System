package com.wagba.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long foodId,
        String foodName,
        String imageUrl,
        /** Price actually charged (the offer price when one applies). */
        BigDecimal price,
        /** List price, present only when an offer is discounting this item. */
        BigDecimal originalPrice,
        int quantity,
        BigDecimal subtotal,
        /** False when the kitchen has taken the item off the menu since it was added. */
        boolean available,
        Long restaurantId
) {
}
