package com.wagba.dto.restaurant;

import com.wagba.entity.enums.RestaurantStatus;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        RestaurantStatus status,
        List<CategoryResponse> categories,
        String cuisine,
        Integer etaMinutes,
        BigDecimal deliveryFee,
        BigDecimal minOrderTotal,
        Double avgRating
) {
}
