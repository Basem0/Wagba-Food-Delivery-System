package com.wagba.dto.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        Long restaurantId,
        String restaurantName,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal totalPrice,
        BigDecimal discountAmount,
        String couponCode,
        AddressResponse deliveryAddress,
        List<OrderItemResponse> items,
        String deliveryStatus,
        String createdAt,
        String customerName,
        String customerPhone,
        Double customerLatitude,
        Double customerLongitude,
        boolean reviewed,
        String paymentMethod,
        boolean paid
) {
}
