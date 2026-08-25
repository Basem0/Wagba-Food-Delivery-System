package com.wagba.dto;

public record AddressRequest(
        String city,
        String street,
        String buildingNumber,
        String apartment,
        String details,
        Double latitude,
        Double longitude
) {
}
