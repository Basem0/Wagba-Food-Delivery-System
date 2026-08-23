package com.wagba.service;

import com.wagba.dto.coupon.AssignCouponRequest;
import com.wagba.dto.coupon.CouponRequest;
import com.wagba.dto.coupon.CouponResponse;
import com.wagba.dto.coupon.UserCouponResponse;
import com.wagba.entity.Coupon;
import com.wagba.entity.User;
import com.wagba.entity.UserCoupon;
import com.wagba.entity.enums.DiscountType;
import com.wagba.repository.CouponRepository;
import com.wagba.repository.UserCouponRepository;
import com.wagba.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    public CouponService(CouponRepository couponRepository,
                         UserCouponRepository userCouponRepository,
                         UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
    }

    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Coupon code already exists");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(request.getCode());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setValue(request.getValue());
        coupon.setMinOrderTotal(request.getMinOrderTotal() != null ? request.getMinOrderTotal() : BigDecimal.ZERO);
        coupon.setActive(request.isActive());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon = couponRepository.save(coupon);
        return toResponse(coupon);
    }

    public void assignCouponToUser(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        if (userCouponRepository.findByUserAndCoupon(user, coupon).isPresent()) {
            return;
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUser(user);
        userCoupon.setCoupon(coupon);
        userCouponRepository.save(userCoupon);
    }

    public List<UserCouponResponse> getUserCoupons(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userCouponRepository.findByUser(user).stream().map(this::toUserCouponResponse).toList();
    }

    public List<CouponResponse> listCoupons() {
        return couponRepository.findAll().stream().map(this::toResponse).toList();
    }

    public BigDecimal applyCoupon(String code, User user, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        UserCoupon userCoupon = userCouponRepository.findByUserAndCoupon(user, coupon)
                .orElseThrow(() -> new RuntimeException("You do not own this coupon"));
        if (userCoupon.isUsed()) {
            throw new RuntimeException("Coupon already used");
        }
        if (!coupon.isActive()) {
            throw new RuntimeException("Coupon is not active");
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Coupon expired");
        }
        if (subtotal.compareTo(coupon.getMinOrderTotal()) < 0) {
            throw new RuntimeException("Order total must be at least " + coupon.getMinOrderTotal());
        }
        BigDecimal discount = computeDiscount(coupon, subtotal);
        userCoupon.setUsed(true);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponRepository.save(userCoupon);
        return discount;
    }

    private BigDecimal computeDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getDiscountType() == DiscountType.FIXED) {
            return coupon.getValue().min(subtotal);
        }
        return subtotal.multiply(coupon.getValue())
                .divide(BigDecimal.valueOf(100));
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getValue(),
                coupon.getMinOrderTotal(),
                coupon.isActive(),
                coupon.getExpiryDate()
        );
    }

    private UserCouponResponse toUserCouponResponse(UserCoupon userCoupon) {
        Coupon c = userCoupon.getCoupon();
        return new UserCouponResponse(
                userCoupon.getId(),
                c.getCode(),
                c.getDescription(),
                c.getDiscountType(),
                c.getValue(),
                c.getMinOrderTotal(),
                c.isActive(),
                c.getExpiryDate(),
                userCoupon.isUsed(),
                userCoupon.getAssignedAt() != null ? userCoupon.getAssignedAt().toString() : null
        );
    }
}
