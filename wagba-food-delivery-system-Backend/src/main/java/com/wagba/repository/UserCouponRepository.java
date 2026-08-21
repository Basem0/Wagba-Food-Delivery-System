package com.wagba.repository;

import com.wagba.entity.Coupon;
import com.wagba.entity.User;
import com.wagba.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUser(User user);

    Optional<UserCoupon> findByUserAndCoupon(User user, Coupon coupon);

    List<UserCoupon> findByUserAndUsedFalse(User user);
}
