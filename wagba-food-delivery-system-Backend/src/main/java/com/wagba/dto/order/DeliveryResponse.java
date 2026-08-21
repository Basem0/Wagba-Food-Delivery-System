package com.wagba.dto.order;

public record DeliveryResponse(
        Long id,
        Long orderId,
        String status,
        Long driverId,
        String acceptedAt,
        String pickedUpAt,
        String deliveredAt
) {
}
