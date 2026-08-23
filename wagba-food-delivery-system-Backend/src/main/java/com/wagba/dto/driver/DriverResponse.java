package com.wagba.dto.driver;

public record DriverResponse(
        Long id,
        String phoneNumber,
        String nationalId,
        String vehicleType,
        String vehicleNumber,
        String licenseNumber,
        Double latitude,
        Double longitude
) {
}
