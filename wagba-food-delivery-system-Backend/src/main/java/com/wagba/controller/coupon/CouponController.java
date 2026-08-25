package com.wagba.controller.coupon;

import com.wagba.dto.coupon.CouponPreview;
import com.wagba.dto.coupon.CouponPreviewRequest;
import com.wagba.dto.coupon.UserCouponResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.CartService;
import com.wagba.service.CouponService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;

    public CouponController(CouponService couponService, CartService cartService) {
        this.couponService = couponService;
        this.cartService = cartService;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<UserCouponResponse> myCoupons() {
        return couponService.getUserCoupons(SecurityUtil.getCurrentUserEmail());
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CouponPreview preview(@RequestBody CouponPreviewRequest req) {
        String email = SecurityUtil.getCurrentUserEmail();
        BigDecimal subtotal = cartService.getCart(email).total();
        return couponService.previewCoupon(req.code(), email, subtotal);
    }
}
