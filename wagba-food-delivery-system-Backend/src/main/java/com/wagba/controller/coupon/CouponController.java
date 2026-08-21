package com.wagba.controller.coupon;

import com.wagba.dto.coupon.UserCouponResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.CouponService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<UserCouponResponse> myCoupons() {
        return couponService.getUserCoupons(SecurityUtil.getCurrentUserEmail());
    }
}
