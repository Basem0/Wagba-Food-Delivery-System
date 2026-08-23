package com.wagba.dto.order;

public record AddressResponse(
        Long id,
        String city,
        String street,
        String buildingNumber,
        String apartment,
        String details,
        Double latitude,
        Double longitude
) {
}
