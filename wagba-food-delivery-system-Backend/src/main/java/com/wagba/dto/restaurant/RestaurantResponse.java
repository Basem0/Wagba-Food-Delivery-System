package com.wagba.dto.restaurant;

import com.wagba.entity.enums.RestaurantStatus;

import java.util.List;

public record RestaurantResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        RestaurantStatus status,
        List<CategoryResponse> categories
) {
}
