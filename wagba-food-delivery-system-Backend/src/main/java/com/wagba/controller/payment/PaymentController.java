package com.wagba.controller.payment;

import com.wagba.dto.cart.CartResponse;
import com.wagba.dto.order.OrderResponse;
import com.wagba.dto.payment.PaymentRequest;
import com.wagba.entity.Order;
import com.wagba.entity.User;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.UserRepository;
import com.wagba.security.SecurityUtil;
import com.wagba.service.CartService;
import com.wagba.service.OrderService;
import com.wagba.service.StripeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;
    private final OrderService orderService;
    private final CartService cartService;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.publishable.key:}")
    private String stripePublishableKey;

    public PaymentController(OrderRepository orderRepository,
                             UserRepository userRepository,
                             StripeService stripeService,
                             OrderService orderService,
                             CartService cartService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        boolean devMode = stripeApiKey == null || stripeApiKey.isBlank();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("publishableKey", devMode ? "" : stripePublishableKey);
        res.put("devMode", devMode);
        return res;
    }

    @PostMapping("/create-intent")
    public Map<String, Object> createIntent(@Valid @RequestBody PaymentRequest request) {
        Order order = requireOwnOrder(request.getOrderId());
        if (order.isPaid()) {
            throw new RuntimeException("This order has already been paid");
        }
        return stripeService.createPaymentIntent(order.getTotalPrice());
    }

    /**
     * Creates a Stripe PaymentIntent for the current user's cart total WITHOUT
     * creating an order first. The order is only placed (and marked paid) after
     * the card payment succeeds on the client.
     */
    @PostMapping("/create-cart-intent")
    public Map<String, Object> createCartIntent() {
        CartResponse cart = cartService.getCart(SecurityUtil.getCurrentUserEmail());
        if (cart.items() == null || cart.items().isEmpty()) {
            throw new RuntimeException("Your cart is empty");
        }
        return stripeService.createPaymentIntent(cart.total());
    }

    /**
     * Records a successful card payment. Without this the order stayed unpaid
     * forever, so the restaurant and driver were never credited.
     */
    @PostMapping("/confirm")
    public OrderResponse confirm(@Valid @RequestBody PaymentRequest request) {
        requireOwnOrder(request.getOrderId());
        return orderService.markPaid(SecurityUtil.getCurrentUserEmail(),
                request.getOrderId(), request.getPaymentReference());
    }

    /**
     * Captures the card payment for an order. Called when the restaurant accepts
     * the order; the card was only authorised at checkout (manual capture).
     */
    @PostMapping("/capture")
    public Map<String, Object> capture(@Valid @RequestBody PaymentRequest request) {
        Order order = requireOwnOrder(request.getOrderId());
        if (!order.isPaid() || order.getPaymentReference() == null) {
            throw new RuntimeException("Order is not paid");
        }
        return stripeService.capturePayment(order.getPaymentReference());
    }

    private Order requireOwnOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        User currentUser = userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!order.getCustomer().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Order does not belong to you");
        }
        return order;
    }
}
