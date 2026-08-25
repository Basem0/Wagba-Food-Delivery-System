package com.wagba.dto.coupon;

import java.math.BigDecimal;

public record CouponPreview(String code, BigDecimal discount, BigDecimal finalTotal) {
}
