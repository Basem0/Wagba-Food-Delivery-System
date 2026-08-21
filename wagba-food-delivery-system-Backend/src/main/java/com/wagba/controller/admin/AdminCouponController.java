package com.wagba.controller.admin;

import com.wagba.dto.coupon.AssignCouponRequest;
import com.wagba.dto.coupon.CouponRequest;
import com.wagba.dto.coupon.CouponResponse;
import com.wagba.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public CouponResponse create(@RequestBody CouponRequest request) {
        return couponService.createCoupon(request);
    }

    @PostMapping("/assign")
    public ResponseEntity<String> assign(@RequestBody AssignCouponRequest request) {
        couponService.assignCouponToUser(request.getUserId(), request.getCode());
        return ResponseEntity.ok("Coupon assigned to user");
    }
}
