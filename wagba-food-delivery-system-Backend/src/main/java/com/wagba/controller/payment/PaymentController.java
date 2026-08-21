package com.wagba.controller.payment;

import com.wagba.dto.payment.PaymentRequest;
import com.wagba.entity.Order;
import com.wagba.entity.User;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.UserRepository;
import com.wagba.security.SecurityUtil;
import com.wagba.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    public PaymentController(OrderRepository orderRepository,
                             UserRepository userRepository,
                             StripeService stripeService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
    }

    @PostMapping("/create-intent")
    public Map<String, Object> createIntent(@RequestBody PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        User currentUser = userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!order.getCustomer().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Order does not belong to you");
        }
        return stripeService.createPaymentIntent(order.getTotalPrice());
    }
}
