package com.wagba.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long foodId,
        String foodName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
) {
}
