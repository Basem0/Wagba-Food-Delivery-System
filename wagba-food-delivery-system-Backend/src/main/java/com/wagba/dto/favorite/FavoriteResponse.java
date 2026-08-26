package com.wagba.dto.favorite;

import java.math.BigDecimal;

public record FavoriteResponse(
    Long id,
    Long restaurantId,
    String restaurantName,
    String cuisine,
    String imageUrl,
    BigDecimal avgRating,
    String createdAt
) {}
