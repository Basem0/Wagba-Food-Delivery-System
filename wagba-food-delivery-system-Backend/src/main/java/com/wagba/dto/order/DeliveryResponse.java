package com.wagba.dto.order;

public record DeliveryResponse(
        Long id,
        Long orderId,
        String status,
        Long driverId,
        String acceptedAt,
        String pickedUpAt,
        String deliveredAt,
        java.math.BigDecimal fee,
        java.math.BigDecimal earning,
        String restaurantName,
        Double restaurantLatitude,
        Double restaurantLongitude,
        String restaurantAddress,
        String customerName,
        Double customerLatitude,
        Double customerLongitude,
        String customerAddress
) {
}
