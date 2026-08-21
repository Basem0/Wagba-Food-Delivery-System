package com.wagba.dto.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        Long restaurantId,
        String restaurantName,
        BigDecimal totalPrice,
        AddressResponse deliveryAddress,
        List<OrderItemResponse> items,
        String deliveryStatus,
        String createdAt
) {
}
