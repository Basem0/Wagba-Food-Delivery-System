package com.wagba.dto.coupon;

import com.wagba.entity.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserCouponResponse(
        Long id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal value,
        BigDecimal minOrderTotal,
        boolean active,
        LocalDate expiryDate,
        boolean used,
        String assignedAt
) {
}
