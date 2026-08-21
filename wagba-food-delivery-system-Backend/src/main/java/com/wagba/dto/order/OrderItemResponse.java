package com.wagba.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long foodId,
        String foodName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
