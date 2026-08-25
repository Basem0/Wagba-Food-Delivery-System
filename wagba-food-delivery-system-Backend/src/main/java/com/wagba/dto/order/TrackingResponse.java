package com.wagba.dto.order;

public record TrackingResponse(
        Long orderId,
        String orderStatus,
        String deliveryStatus,
        String restaurantName,
        DriverTrackingInfo driver,
        String createdAt,
        Double customerLatitude,
        Double customerLongitude,
        Double restaurantLatitude,
        Double restaurantLongitude
) {
}
