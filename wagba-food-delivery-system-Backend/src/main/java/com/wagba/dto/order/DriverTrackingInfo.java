package com.wagba.dto.order;

public record DriverTrackingInfo(
        Long id,
        String name,
        String phone,
        String vehicleType,
        String vehicleNumber,
        Double latitude,
        Double longitude,
        String locationUpdatedAt
) {
}
