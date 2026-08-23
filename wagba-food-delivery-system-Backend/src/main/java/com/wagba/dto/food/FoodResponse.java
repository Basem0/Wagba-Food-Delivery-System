package com.wagba.dto.food;

import java.math.BigDecimal;

public record FoodResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available,
        Long categoryId,
        String categoryName,
        BigDecimal discountPrice,
        boolean offer
) {
}
