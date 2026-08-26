package com.wagba.dto.review;

public record ReviewResponse(
        Long id,
        Integer rating,
        String comment,
        String customerName,
        Long orderId,
        Long restaurantId,
        String restaurantName,
        Long driverId,
        String createdAt,
        String type,
        Long targetId
) {
}
