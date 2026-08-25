package com.wagba.service;

import com.wagba.dto.coupon.CouponPreview;
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
import java.math.RoundingMode;
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

    /** Shared validation for apply/preview - keeps the two paths from drifting apart. */
    private UserCoupon validateForUse(String code, User user, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Coupon code is required");
        }
        Coupon coupon = couponRepository.findByCode(code.trim())
                .orElseThrow(() -> new RuntimeException("Coupon \"" + code.trim() + "\" does not exist"));
        UserCoupon userCoupon = userCouponRepository.findByUserAndCoupon(user, coupon)
                .orElseThrow(() -> new RuntimeException("This coupon is not available on your account"));
        if (userCoupon.isUsed()) {
            throw new RuntimeException("You have already used this coupon");
        }
        if (!coupon.isActive()) {
            throw new RuntimeException("This coupon is no longer active");
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("This coupon expired on " + coupon.getExpiryDate());
        }
        if (coupon.getMinOrderTotal() != null && subtotal.compareTo(coupon.getMinOrderTotal()) < 0) {
            throw new RuntimeException("This coupon needs a minimum order of "
                    + coupon.getMinOrderTotal().setScale(2, RoundingMode.HALF_UP) + " EGP");
        }
        return userCoupon;
    }

    public BigDecimal applyCoupon(String code, User user, BigDecimal subtotal) {
        UserCoupon userCoupon = validateForUse(code, user, subtotal);
        BigDecimal discount = computeDiscount(userCoupon.getCoupon(), subtotal);
        userCoupon.setUsed(true);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponRepository.save(userCoupon);
        return discount;
    }

    /**
     * Hands a coupon back to the customer, used when an order that consumed it is
     * cancelled. Silently does nothing if there is nothing to release.
     */
    public void releaseCoupon(String code, User user) {
        if (code == null || code.isBlank()) return;
        couponRepository.findByCode(code.trim())
                .flatMap(coupon -> userCouponRepository.findByUserAndCoupon(user, coupon))
                .ifPresent(uc -> {
                    if (uc.isUsed()) {
                        uc.setUsed(false);
                        uc.setUsedAt(null);
                        userCouponRepository.save(uc);
                    }
                });
    }

    public CouponPreview previewCoupon(String code, String email, BigDecimal subtotal) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserCoupon userCoupon = validateForUse(code, user, subtotal);
        BigDecimal discount = computeDiscount(userCoupon.getCoupon(), subtotal);
        BigDecimal finalTotal = subtotal.subtract(discount).max(BigDecimal.ZERO);
        return new CouponPreview(userCoupon.getCoupon().getCode(), discount, finalTotal);
    }

    private BigDecimal computeDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getValue() == null) return BigDecimal.ZERO;
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.FIXED) {
            discount = coupon.getValue();
        } else {
            // Cap at 100% so a mis-keyed percentage cannot produce a negative total.
            BigDecimal percent = coupon.getValue().min(BigDecimal.valueOf(100));
            discount = subtotal.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return discount.min(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
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
